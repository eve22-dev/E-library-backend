package com.e_library.security;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Fala com o Supabase Auth (onde as contas e senhas existem).
 * Variáveis de ambiente necessárias: SUPABASE_URL e SUPABASE_ANON_KEY.
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

    // Confere e-mail e senha. true = senha correta.
    public boolean passwordIsValid(String email, String password) {

        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SUPABASE_URL / SUPABASE_ANON_KEY não configuradas no servidor"
            );
        }

        try {
            String body = mapper.writeValueAsString(
                    Map.of("email", email, "password", password)
            );

            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(supabaseUrl + "/auth/v1/token?grant_type=password"))
                    .header("apikey", anonKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();

            if (status == 200) {
                return true;
            }

            if (status == 400 || status == 401 || status == 422) {
                return false;
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Falha ao consultar o serviço de autenticação"
            );

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Falha ao consultar o serviço de autenticação"
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Falha ao consultar o serviço de autenticação"
            );
        }
    }

    // Retorna o e-mail dono de um token do Supabase, ou null se for inválido/expirado.
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

            cache.values().removeIf(c -> c.expiresAt() <= now);
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
