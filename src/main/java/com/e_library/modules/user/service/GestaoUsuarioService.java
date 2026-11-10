package com.e_library.modules.user.service;

import com.e_library.modules.user.entity.UserEntity;
import com.e_library.modules.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// Manutenção de usuários (somente TI): listar com filtros, editar, inativar e reativar
@Service
public class GestaoUsuarioService {

    public record Dados(String nome, String papel, String rgm, String curso) {}

    private static final Set<String> PAPEIS = Set.of("aluno", "professor", "bibliotecario", "ti");

    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;

    public GestaoUsuarioService(JdbcTemplate jdbc, UserRepository userRepository) {
        this.jdbc = jdbc;
        this.userRepository = userRepository;
    }

    public List<Map<String, Object>> listar(String nome, String email, String rgm, String papel, String situacao) {

        StringBuilder sql = new StringBuilder(
                "SELECT id, nome, email, papel, ativo, rgm, curso, criado_em FROM usuarios WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        filtrarTexto(sql, params, "nome", nome);
        filtrarTexto(sql, params, "email", email);
        filtrarTexto(sql, params, "rgm", rgm);

        if (papel != null && !papel.isBlank()) {
            sql.append(" AND papel = ?");
            params.add(papel.trim().toLowerCase());
        }
        if ("ativo".equalsIgnoreCase(situacao)) {
            sql.append(" AND ativo = true");
        } else if ("inativo".equalsIgnoreCase(situacao)) {
            sql.append(" AND ativo = false");
        }

        sql.append(" ORDER BY nome LIMIT 500");

        return jdbc.query(sql.toString(), (rs, i) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rs.getObject("id"));
            m.put("nome", rs.getString("nome"));
            m.put("email", rs.getString("email"));
            m.put("papel", rs.getString("papel"));
            m.put("ativo", rs.getBoolean("ativo"));
            m.put("rgm", rs.getString("rgm"));
            m.put("curso", rs.getString("curso"));
            m.put("criadoEm", rs.getTimestamp("criado_em"));
            return m;
        }, params.toArray());
    }

    public void atualizar(UUID alvo, String emailDoAdmin, Dados d) {

        garantirExiste(alvo);

        String nome = d.nome() == null ? "" : d.nome().trim();
        String papel = d.papel() == null ? "" : d.papel().trim().toLowerCase();
        String rgm = vazioParaNulo(d.rgm());
        String curso = vazioParaNulo(d.curso());

        if (nome.length() < 3 || nome.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome completo (de 3 a 120 caracteres).");
        }
        if (!PAPEIS.contains(papel)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Papel inválido. Use aluno, professor, bibliotecario ou ti.");
        }
        if (alvo.equals(idDe(emailDoAdmin)) && !"ti".equals(papel)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você não pode tirar o seu próprio acesso de TI.");
        }
        if (rgm != null && contar("SELECT COUNT(*) FROM usuarios WHERE rgm = ? AND id <> ?", rgm, alvo) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe outro usuário com este RGM.");
        }

        jdbc.update("UPDATE usuarios SET nome = ?, papel = ?, rgm = ?, curso = ? WHERE id = ?",
                nome, papel, rgm, curso, alvo);
    }

    public void definirAtivo(UUID alvo, String emailDoAdmin, boolean ativo) {

        garantirExiste(alvo);

        if (!ativo && alvo.equals(idDe(emailDoAdmin))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você não pode inativar a própria conta.");
        }

        jdbc.update("UPDATE usuarios SET ativo = ? WHERE id = ?", ativo, alvo);
    }

    private void garantirExiste(UUID id) {
        if (contar("SELECT COUNT(*) FROM usuarios WHERE id = ?", id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
        }
    }

    private UUID idDe(String email) {
        Optional<UserEntity> u = userRepository.findByEmail(email);
        return u.map(UserEntity::getId).orElse(null);
    }

    private void filtrarTexto(StringBuilder sql, List<Object> params, String coluna, String valor) {
        if (valor == null || valor.isBlank()) return;
        sql.append(" AND UPPER(").append(coluna).append(") LIKE ?");
        params.add("%" + valor.trim().toUpperCase() + "%");
    }

    private int contar(String sql, Object... args) {
        Integer total = jdbc.queryForObject(sql, Integer.class, args);
        return total == null ? 0 : total;
    }

    private String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
