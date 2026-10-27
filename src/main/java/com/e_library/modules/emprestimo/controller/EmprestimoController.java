package com.e_library.modules.emprestimo.controller;

import com.e_library.modules.emprestimo.dto.EmprestimoResponseDTO;
import com.e_library.modules.emprestimo.entity.EmprestimoEntity;
import com.e_library.modules.emprestimo.repository.EmprestimoRepository;
import com.e_library.modules.emprestimo.service.EmprestimoService;
import com.e_library.modules.exemplar.entity.ExemplarEntity;
import com.e_library.modules.livro.entity.LivroEntity;
import com.e_library.modules.livro.repository.LivroRepository;
import com.e_library.modules.multa.entity.MultaEntity;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/emprestimos")
@CrossOrigin(origins = "*") // Permite que o front-end acesse a API
public class EmprestimoController {

    private final EmprestimoRepository emprestimoRepository;
    private final LivroRepository livroRepository;
    private final EmprestimoService emprestimoService;

    public EmprestimoController(EmprestimoRepository emprestimoRepository,
                                LivroRepository livroRepository,
                                EmprestimoService emprestimoService) {
        this.emprestimoRepository = emprestimoRepository;
        this.livroRepository = livroRepository;
        this.emprestimoService = emprestimoService;
    }

    // =========================================================
    // ROTAS DO USUÁRIO
    // =========================================================

    // Reservar um livro (diminui o estoque)
    @PostMapping("/reservar")
    public ResponseEntity<?> reservar(@RequestBody Map<String, String> payload, Authentication auth) {
        UUID livroId;
        try {
            livroId = UUID.fromString(payload.get("livroId"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Livro inválido.");
        }

        try {
            EmprestimoEntity criado = emprestimoService.reservar(auth.getName(), livroId);
            return ResponseEntity.ok(new EmprestimoResponseDTO(criado));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<String> cancelarReserva(@PathVariable @NonNull UUID id, Authentication auth) {
        EmprestimoEntity emprestimo = emprestimoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado."));

        boolean ehBibliotecario = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_BIBLIOTECARIO"));

        boolean ehDono = emprestimo.getUsuario() != null
            && auth.getName().equalsIgnoreCase(emprestimo.getUsuario().getEmail());

        if (!ehDono && !ehBibliotecario) {
            return ResponseEntity.status(403).body("Você não pode cancelar a reserva de outro usuário.");
        }

        // Só cancela enquanto o livro ainda não foi retirado
        if (!"solicitado".equalsIgnoreCase(emprestimo.getStatus())) {
            return ResponseEntity.badRequest().body("Só é possível cancelar reservas que ainda não foram retiradas.");
        }

        emprestimo.setStatus("cancelado");
        emprestimoRepository.save(emprestimo);

        // Devolve a unidade ao estoque, sem passar do total
        LivroEntity livro = emprestimo.getLivro();
        if (livro != null && livro.getQtdDisponivel() != null) {
            int novo = livro.getQtdDisponivel() + 1;
            if (livro.getQtdTotal() != null) novo = Math.min(novo, livro.getQtdTotal());
            livro.setQtdDisponivel(novo);
            livroRepository.save(livro);
        }

        return ResponseEntity.ok("Reserva cancelada com sucesso!");
    }

    // Empréstimos e reservas do usuário logado (tela Meus Empréstimos e avisos da home)
    @GetMapping("/meus")
    public ResponseEntity<List<EmprestimoEntity>> meusEmprestimos(Authentication auth) {
        return ResponseEntity.ok(emprestimoRepository.findByUsuario_EmailOrderByDataPrevistaAsc(auth.getName()));
    }

    // Pedir renegociação do prazo (empréstimo quase vencendo ou já vencido)
    @PostMapping("/{id}/renegociar")
    public ResponseEntity<String> renegociar(@PathVariable @NonNull UUID id, Authentication auth) {
        try {
            emprestimoService.solicitarRenegociacao(auth.getName(), id);
            return ResponseEntity.ok("Pedido enviado! Aguarde a resposta do bibliotecário.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    // =========================================================
    // ROTAS DO BIBLIOTECÁRIO (Para funcionar o Painel)
    // =========================================================

    // Lista as solicitações pendentes para o bibliotecário ver
    @GetMapping("/solicitados")
    public ResponseEntity<List<EmprestimoEntity>> listarSolicitados() {
        List<EmprestimoEntity> solicitados = emprestimoRepository.findByStatus("solicitado");
        return ResponseEntity.ok(solicitados);
    }

    // Livros emprestados que ainda não voltaram (para registrar a devolução)
    @GetMapping("/retirados")
    public ResponseEntity<List<EmprestimoEntity>> listarRetirados() {
        return ResponseEntity.ok(
            emprestimoRepository.findByStatusInOrderByDataPrevistaAsc(List.of("retirado", "atrasado")));
    }

    // Livros extraviados aguardando reposição (ou baixa)
    @GetMapping("/extraviados")
    public ResponseEntity<List<EmprestimoEntity>> listarExtraviados() {
        return ResponseEntity.ok(emprestimoRepository.findByStatus("extraviado"));
    }

    // Pedidos de renegociação aguardando decisão
    @GetMapping("/renegociacoes")
    public ResponseEntity<List<EmprestimoEntity>> listarRenegociacoes() {
        return ResponseEntity.ok(emprestimoRepository.findByRenegociacaoStatus("pendente"));
    }

    @PutMapping("/{id}/renegociacao/aprovar")
    public ResponseEntity<String> aprovarRenegociacao(@PathVariable @NonNull UUID id) {
        try {
            LocalDate novoPrazo = emprestimoService.decidirRenegociacao(id, true);
            String data = novoPrazo.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "BR")));
            return ResponseEntity.ok("Renegociação aprovada! Novo prazo de devolução: " + data);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PutMapping("/{id}/renegociacao/recusar")
    public ResponseEntity<String> recusarRenegociacao(@PathVariable @NonNull UUID id) {
        try {
            emprestimoService.decidirRenegociacao(id, false);
            return ResponseEntity.ok("Renegociação recusada.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    // Botão "Registrar devolução": o livro voltou para a biblioteca (gera multa se houve atraso)
    @PutMapping("/{id}/devolver")
    public ResponseEntity<String> registrarDevolucao(@PathVariable @NonNull UUID id) {
        try {
            MultaEntity multa = emprestimoService.registrarDevolucao(id);

            if (multa == null) {
                return ResponseEntity.ok("Devolução registrada com sucesso!");
            }

            return ResponseEntity.ok(String.format(new Locale("pt", "BR"),
                "Devolução registrada. Atraso de %d dia(s): multa de R$ %.2f gerada para o usuário.",
                multa.getDiasAtraso(), multa.getValor()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    // Livro extraviado: o atraso congela e abre 30 dias para o usuário repor o exemplar
    @PutMapping("/{id}/extravio")
    public ResponseEntity<String> registrarExtravio(@PathVariable @NonNull UUID id) {
        try {
            MultaEntity multa = emprestimoService.registrarExtravio(id);

            String msg = "Extravio registrado. O usuário tem 30 dias para entregar um exemplar novo.";

            if (multa != null) {
                msg += String.format(new Locale("pt", "BR"),
                    " Multa do atraso até hoje (%d dia(s)): R$ %.2f.", multa.getDiasAtraso(), multa.getValor());
            }

            return ResponseEntity.ok(msg);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    // Usuário entregou um exemplar novo: fecha a ocorrência de extravio
    @PutMapping("/{id}/reposicao")
    public ResponseEntity<String> registrarReposicao(@PathVariable @NonNull UUID id,
                                                     @RequestBody Map<String, String> payload) {
        try {
            emprestimoService.registrarReposicao(id, payload.get("codigoExemplar"));
            return ResponseEntity.ok("Reposição registrada. O exemplar novo já está disponível no acervo.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    // Livro não reposto: baixa definitiva do exemplar
    @PutMapping("/{id}/baixa")
    public ResponseEntity<String> registrarBaixa(@PathVariable @NonNull UUID id) {
        try {
            emprestimoService.registrarBaixa(id);
            return ResponseEntity.ok("Baixa registrada. O exemplar saiu do total do acervo.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    // Botão "Entregar": o bibliotecário bipa o código da cópia física que está saindo
    @PutMapping("/{id}/retirar")
    public ResponseEntity<String> marcarComoRetirado(@PathVariable @NonNull UUID id,
                                                     @RequestBody Map<String, String> payload) {
        try {
            ExemplarEntity exemplar = emprestimoService.entregar(id, payload.get("codigoExemplar"));
            return ResponseEntity.ok("Entrega registrada! Exemplar " + exemplar.getCodigo() + " com o usuário.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }
}
