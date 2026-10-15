package com.e_library.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Pergunta ao Supabase "de quem é este token?". Assim o back aceita o token do
 * login do Supabase, qualquer que seja o tipo de assinatura dele.
 * Precisa das variáveis de ambiente SUPABASE_URL e SUPABASE_ANON_KEY.
 */
@Component
public class SupabaseAuthClient {

    private static final long CACHE_MS = 60_000; // lembra o resultado por 1 minuto

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, CachedEmail> cache = new ConcurrentHashMap<>();

    private final String supabaseUrl;
    private final String anonKey;

    private record CachedEmail(String email, long expiresAt) {}

    public SupabaseAuthClient(
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.anon-key:}") String anonKey
    ) {
        this.supabaseUrl = supabaseUrl;
        this.anonKey = anonKey;
    }

    /** Retorna o e-mail dono do token, ou null se o token for inválido/expirado. */
    public String emailFromAccessToken(String token) {

        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            return null;
        }

        long now = System.currentTimeMillis();

        CachedEmail hit = cache.get(token);
        if (hit != null && hit.expiresAt() > now) {
            return hit.email();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(supabaseUrl + "/auth/v1/user"))
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return null;
            }

            JsonNode email = mapper.readTree(response.body()).get("email");

            if (email == null || email.isNull()) {
                return null;
            }

            cache.values().removeIf(c -> c.expiresAt() <= now); // limpa os vencidos
            cache.put(token, new CachedEmail(email.asText(), now + CACHE_MS));

            return email.asText();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
