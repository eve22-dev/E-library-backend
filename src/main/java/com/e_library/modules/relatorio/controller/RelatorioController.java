package com.e_library.modules.relatorio.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// Todos os relatórios são exclusivos do bibliotecário (ver SecurityConfig).
// Filtro de período opcional: ?de=2026-01-01&ate=2026-12-31
@RestController
@RequestMapping("/api/relatorios")
@CrossOrigin(origins = "*")
public class RelatorioController {

    private final JdbcTemplate jdbc;

    public RelatorioController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Números gerais para os cartões do topo do dashboard
    @GetMapping("/resumo")
    public ResponseEntity<Map<String, Object>> resumo() {
        return ResponseEntity.ok(jdbc.queryForMap(
            "SELECT " +
            " (SELECT COUNT(*) FROM livros WHERE ativo = true) AS livros_ativos, " +
            " (SELECT COUNT(*) FROM exemplares WHERE status IN ('disponivel', 'emprestado')) AS exemplares_no_acervo, " +
            " (SELECT COUNT(*) FROM emprestimos WHERE status IN ('retirado', 'atrasado')) AS emprestimos_em_andamento, " +
            " (SELECT COUNT(*) FROM emprestimos WHERE status IN ('retirado', 'atrasado') AND data_prevista < CURRENT_DATE) AS emprestimos_atrasados, " +
            " (SELECT COALESCE(SUM(valor), 0) FROM multas WHERE status = 'pendente') AS multas_pendentes, " +
            " (SELECT COUNT(*) FROM emprestimos WHERE status = 'extraviado') AS extravios_aguardando"));
    }

    // Livros x empréstimos
    @GetMapping("/livros-emprestimos")
    public ResponseEntity<List<Map<String, Object>>> livrosEmprestimos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {

        return ResponseEntity.ok(jdbc.queryForList(
            "SELECT l.titulo, l.autor, COALESCE(l.editora, '') AS editora, " +
            "  COUNT(*) AS total_emprestimos, " +
            "  COUNT(*) FILTER (WHERE e.status IN ('retirado', 'atrasado')) AS em_andamento, " +
            "  COUNT(*) FILTER (WHERE e.status = 'devolvido') AS devolvidos " +
            "FROM emprestimos e JOIN livros l ON l.id = e.livro_id " +
            "WHERE e.status NOT IN ('solicitado', 'cancelado') AND e.data_retirada BETWEEN ? AND ? " +
            "GROUP BY l.id, l.titulo, l.autor, l.editora " +
            "ORDER BY total_emprestimos DESC, l.titulo",
            inicio(de), fim(ate)));
    }

    // Usuários x empréstimos
    @GetMapping("/usuarios-emprestimos")
    public ResponseEntity<List<Map<String, Object>>> usuariosEmprestimos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {

        return ResponseEntity.ok(jdbc.queryForList(
            "SELECT u.nome, u.email, u.papel, " +
            "  COUNT(*) AS total_emprestimos, " +
            "  COUNT(*) FILTER (WHERE e.status IN ('retirado', 'atrasado')) AS em_andamento, " +
            "  COUNT(*) FILTER (WHERE e.status = 'devolvido') AS devolvidos, " +
            "  COUNT(*) FILTER (WHERE e.status IN ('extraviado', 'reposto', 'baixado')) AS extravios " +
            "FROM emprestimos e JOIN usuarios u ON u.id = e.user_id " +
            "WHERE e.status NOT IN ('solicitado', 'cancelado') AND e.data_retirada BETWEEN ? AND ? " +
            "GROUP BY u.id, u.nome, u.email, u.papel " +
            "ORDER BY total_emprestimos DESC, u.nome",
            inicio(de), fim(ate)));
    }

    // Usuários x multas
    @GetMapping("/usuarios-multas")
    public ResponseEntity<List<Map<String, Object>>> usuariosMultas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {

        return ResponseEntity.ok(jdbc.queryForList(
            "SELECT u.nome, u.email, u.papel, " +
            "  COUNT(*) AS qtd_multas, " +
            "  COALESCE(SUM(m.dias_atraso), 0) AS dias_atraso_total, " +
            "  COALESCE(SUM(m.valor), 0) AS valor_total, " +
            "  COALESCE(SUM(m.valor) FILTER (WHERE m.status = 'pendente'), 0) AS valor_pendente, " +
            "  COALESCE(SUM(m.valor) FILTER (WHERE m.status = 'paga'), 0) AS valor_pago " +
            "FROM multas m JOIN usuarios u ON u.id = m.user_id " +
            "WHERE m.criada_em BETWEEN ? AND ? " +
            "GROUP BY u.id, u.nome, u.email, u.papel " +
            "ORDER BY valor_total DESC, u.nome",
            inicio(de), fim(ate)));
    }

    // Livros x ocorrências de extravio
    @GetMapping("/livros-extravios")
    public ResponseEntity<List<Map<String, Object>>> livrosExtravios(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {

        return ResponseEntity.ok(jdbc.queryForList(
            "SELECT l.titulo, l.autor, " +
            "  COUNT(*) AS extravios, " +
            "  COUNT(*) FILTER (WHERE e.status = 'extraviado') AS aguardando_reposicao, " +
            "  COUNT(*) FILTER (WHERE e.status = 'reposto') AS repostos, " +
            "  COUNT(*) FILTER (WHERE e.status = 'baixado') AS baixados " +
            "FROM emprestimos e JOIN livros l ON l.id = e.livro_id " +
            "WHERE e.status IN ('extraviado', 'reposto', 'baixado') AND e.data_retirada BETWEEN ? AND ? " +
            "GROUP BY l.id, l.titulo, l.autor " +
            "ORDER BY extravios DESC, l.titulo",
            inicio(de), fim(ate)));
    }

    private Date inicio(LocalDate de) {
        return Date.valueOf(de != null ? de : LocalDate.of(2000, 1, 1));
    }

    private Date fim(LocalDate ate) {
        return Date.valueOf(ate != null ? ate : LocalDate.of(2999, 12, 31));
    }
}
