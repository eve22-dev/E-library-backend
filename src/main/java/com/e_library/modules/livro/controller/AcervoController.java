package com.e_library.modules.livro.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Acervo completo para o bibliotecário (ativos e inativos), com filtros. Rota restrita (ver SecurityConfig).
@RestController
@RequestMapping("/api/livros")
@CrossOrigin(origins = "*")
public class AcervoController {

    private final JdbcTemplate jdbc;

    public AcervoController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/acervo")
    public ResponseEntity<List<Map<String, Object>>> acervo(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String autor,
            @RequestParam(required = false) String editora,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String situacao) {

        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT l.id, l.titulo, l.autor, l.editora, l.isbn, l.ativo, l.motivo_inatividade, " +
                "l.qtd_total, l.qtd_disponivel, c.nome AS categoria " +
                "FROM livros l " +
                "LEFT JOIN categorias c ON c.id = l.categoria_id " +
                "LEFT JOIN exemplares x ON x.livro_id = l.id " +
                "WHERE 1 = 1");

        List<Object> params = new ArrayList<>();

        filtrarTexto(sql, params, "l.titulo", titulo);
        filtrarTexto(sql, params, "l.autor", autor);
        filtrarTexto(sql, params, "l.editora", editora);
        filtrarTexto(sql, params, "REPLACE(REPLACE(l.isbn, '-', ''), ' ', '')",
                isbn == null ? null : isbn.replace("-", "").replace(" ", ""));
        filtrarTexto(sql, params, "x.codigo", codigo);

        if ("ativo".equalsIgnoreCase(situacao)) {
            sql.append(" AND l.ativo = true");
        } else if ("inativo".equalsIgnoreCase(situacao)) {
            sql.append(" AND l.ativo = false");
        }

        sql.append(" ORDER BY l.titulo LIMIT 500");

        List<Map<String, Object>> lista = jdbc.query(sql.toString(), (rs, i) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rs.getObject("id"));
            m.put("titulo", rs.getString("titulo"));
            m.put("autor", rs.getString("autor"));
            m.put("editora", rs.getString("editora"));
            m.put("isbn", rs.getString("isbn"));
            m.put("ativo", rs.getBoolean("ativo"));
            m.put("motivoInatividade", rs.getString("motivo_inatividade"));
            m.put("qtdTotal", rs.getInt("qtd_total"));
            m.put("qtdDisponivel", rs.getInt("qtd_disponivel"));
            m.put("categoria", rs.getString("categoria"));
            return m;
        }, params.toArray());

        return ResponseEntity.ok(lista);
    }

    // Busca sem diferenciar maiúsculas e acentos de caixa (contém o texto digitado)
    private void filtrarTexto(StringBuilder sql, List<Object> params, String coluna, String valor) {
        if (valor == null || valor.isBlank()) return;
        sql.append(" AND UPPER(").append(coluna).append(") LIKE ?");
        params.add("%" + valor.trim().toUpperCase() + "%");
    }
}
