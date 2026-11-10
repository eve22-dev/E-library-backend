package com.e_library.modules.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Cria usuários de verdade: o login no Supabase Auth (que gera o id) e a linha em usuarios com esse mesmo id.
 * Usa a chave service_role (variável SUPABASE_SERVICE_ROLE_KEY). Se o perfil falhar, o login é desfeito.
 */
@Service
public class CriacaoUsuarioService {

    public record Novo(String nome, String email, String senha, String papel, String rgm, String curso) {}

    private static final Set<String> PAPEIS = Set.of("aluno", "professor", "bibliotecario", "ti");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final JdbcTemplate jdbc;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String supabaseUrl;
    private final String serviceKey;

    public CriacaoUsuarioService(JdbcTemplate jdbc,
                                 @Value("${supabase.url:}") String supabaseUrl,
                                 @Value("${supabase.service-role-key:}") String serviceKey) {
        this.jdbc = jdbc;
        this.supabaseUrl = supabaseUrl;
        this.serviceKey = serviceKey;
    }

    public UUID criar(Novo dados) {
        String nome = dados.nome() == null ? "" : dados.nome().trim();
        String email = dados.email() == null ? "" : dados.email().trim().toLowerCase();
        String senha = dados.senha() == null ? "" : dados.senha();
        String papel = dados.papel() == null ? "" : dados.papel().trim().toLowerCase();
        String rgm = vazioParaNulo(dados.rgm());
        String curso = vazioParaNulo(dados.curso());

        if (nome.length() < 3 || nome.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome completo (de 3 a 120 caracteres).");
        }
        if (email.length() > 160 || !EMAIL.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um e-mail válido.");
        }
        if (senha.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha precisa ter pelo menos 8 caracteres.");
        }
        if (!PAPEIS.contains(papel)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Papel inválido. Use aluno, professor, bibliotecario ou ti.");
        }

        if (contar("SELECT COUNT(*) FROM usuarios WHERE email = ?", email) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com este e-mail.");
        }
        if (rgm != null && contar("SELECT COUNT(*) FROM usuarios WHERE rgm = ?", rgm) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com este RGM.");
        }

        // 1) login no Supabase (ele gera o id)
        UUID id = criarLoginNoSupabase(nome, email, senha);

        // 2) perfil na tabela usuarios, com o mesmo id
        try {
            jdbc.update(
                    "INSERT INTO usuarios (id, nome, email, papel, ativo, rgm, curso) VALUES (?, ?, ?, ?, true, ?, ?)",
                    id, nome, email, papel, rgm, curso);
        } catch (RuntimeException e) {
            removerLoginNoSupabase(id);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível salvar o perfil. Nenhum usuário foi criado.");
        }

        return id;
    }

    private UUID criarLoginNoSupabase(String nome, String email, String senha) {

        if (supabaseUrl.isBlank() || serviceKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "SUPABASE_URL ou SUPABASE_SERVICE_ROLE_KEY não configuradas no servidor.");
        }

        try {
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("email", email);
            corpo.put("password", senha);
            corpo.put("email_confirm", true);
            corpo.put("user_metadata", Map.of("name", nome));

            HttpRequest request = HttpRequest.newBuilder(URI.create(supabaseUrl + "/auth/v1/admin/users"))
                    .header("apikey", serviceKey)
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(corpo)))
                    .build();

            HttpResponse<String> resposta = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (resposta.statusCode() == 200 || resposta.statusCode() == 201) {
                JsonNode id = mapper.readTree(resposta.body()).get("id");
                return UUID.fromString(id.asText());
            }

            if (resposta.body().toLowerCase().contains("already")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um login com este e-mail.");
            }

            System.err.println("Supabase Admin respondeu " + resposta.statusCode() + ": " + resposta.body());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível criar o login agora. Tente novamente.");

        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Operação interrompida. Tente novamente.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível criar o login agora. Tente novamente.");
        }
    }

    // Desfaz o login quando o perfil não pôde ser salvo
    private void removerLoginNoSupabase(UUID id) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(supabaseUrl + "/auth/v1/admin/users/" + id))
                    .header("apikey", serviceKey)
                    .header("Authorization", "Bearer " + serviceKey)
                    .DELETE()
                    .build();
            http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Não foi possível desfazer o login " + id + ": " + e.getMessage());
        }
    }

    // Troca a senha de um login existente (o TI redefine a senha de um usuário)
    public void redefinirSenha(UUID id, String senha) {

        if (senha == null || senha.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha precisa ter pelo menos 8 caracteres.");
        }
        if (supabaseUrl.isBlank() || serviceKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "SUPABASE_URL ou SUPABASE_SERVICE_ROLE_KEY não configuradas no servidor.");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(supabaseUrl + "/auth/v1/admin/users/" + id))
                    .header("apikey", serviceKey)
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("Content-Type", "application/json")
                    .method("PUT", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("password", senha))))
                    .build();

            HttpResponse<String> resposta = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (resposta.statusCode() != 200) {
                System.err.println("Supabase Admin (senha) respondeu " + resposta.statusCode() + ": " + resposta.body());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível redefinir a senha agora.");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Operação interrompida. Tente novamente.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível redefinir a senha agora.");
        }
    }

    private int contar(String sql, Object valor) {
        Integer total = jdbc.queryForObject(sql, Integer.class, valor);
        return total == null ? 0 : total;
    }

    private String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
