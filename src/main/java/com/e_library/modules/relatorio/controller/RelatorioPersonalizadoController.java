package com.e_library.modules.relatorio.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

// Relatórios montados pelo bibliotecário: escolhe a base e as colunas. Rotas restritas (ver SecurityConfig).
@RestController
@RequestMapping("/api/relatorios/personalizados")
@CrossOrigin(origins = "*")
public class RelatorioPersonalizadoController {

    private static final Set<String> BASES = Set.of(
            "livros-emprestimos", "usuarios-emprestimos", "usuarios-multas", "livros-extravios");

    private static final int MAX_COLUNAS = 15;

    public record NovoRelatorio(String nome, String base, List<String> colunas) {}

    private final JdbcTemplate jdbc;

    public RelatorioPersonalizadoController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar() {
        List<Map<String, Object>> lista = jdbc.query(
                "SELECT id, nome, base, colunas, criado_em FROM relatorios_personalizados ORDER BY criado_em DESC",
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getObject("id"));
                    m.put("nome", rs.getString("nome"));
                    m.put("base", rs.getString("base"));
                    m.put("colunas", Arrays.asList(rs.getString("colunas").split(",")));
                    m.put("criadoEm", rs.getTimestamp("criado_em"));
                    return m;
                });
        return ResponseEntity.ok(lista);
    }

    @PostMapping
    public ResponseEntity<String> criar(@RequestBody NovoRelatorio pedido, Authentication auth) {

        String nome = pedido.nome() == null ? "" : pedido.nome().trim();

        if (nome.length() < 3 || nome.length() > 80) {
            return ResponseEntity.badRequest().body("O nome deve ter entre 3 e 80 caracteres.");
        }

        if (pedido.base() == null || !BASES.contains(pedido.base())) {
            return ResponseEntity.badRequest().body("Escolha uma base de relatório válida.");
        }

        LinkedHashSet<String> colunas = new LinkedHashSet<>();
        if (pedido.colunas() != null) {
            for (String c : pedido.colunas()) {
                if (c != null && c.matches("[a-z_]{1,40}")) colunas.add(c);
            }
        }

        if (colunas.isEmpty() || colunas.size() > MAX_COLUNAS) {
            return ResponseEntity.badRequest().body("Escolha de 1 a " + MAX_COLUNAS + " colunas.");
        }

        jdbc.update(
                "INSERT INTO relatorios_personalizados (id, nome, base, colunas, criado_por) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), nome, pedido.base(), String.join(",", colunas), auth.getName());

        return ResponseEntity.ok("Relatório salvo!");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> excluir(@PathVariable @NonNull UUID id) {
        int removidos = jdbc.update("DELETE FROM relatorios_personalizados WHERE id = ?", id);

        if (removidos == 0) {
            return ResponseEntity.status(404).body("Relatório não encontrado.");
        }

        return ResponseEntity.ok("Relatório excluído.");
    }
}
