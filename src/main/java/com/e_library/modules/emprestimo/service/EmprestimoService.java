package com.e_library.modules.emprestimo.service;

import com.e_library.modules.emprestimo.entity.EmprestimoEntity;
import com.e_library.modules.emprestimo.repository.EmprestimoRepository;
import com.e_library.modules.livro.entity.LivroEntity;
import com.e_library.modules.livro.repository.LivroRepository;
import com.e_library.modules.multa.entity.MultaEntity;
import com.e_library.modules.multa.repository.MultaRepository;
import com.e_library.modules.user.entity.UserEntity;
import com.e_library.modules.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class EmprestimoService {

    // RN02: multa de R$ 2,00 por dia de atraso
    public static final BigDecimal VALOR_MULTA_POR_DIA = new BigDecimal("2.00");

    // Renegociação só pode ser pedida quando faltam até 3 dias para vencer (ou já venceu)
    private static final int DIAS_ANTES_PARA_RENEGOCIAR = 3;

    // Quando o bibliotecário aprova, o prazo é estendido por 7 dias
    private static final int DIAS_EXTENSAO_RENEGOCIACAO = 7;

    private final EmprestimoRepository emprestimoRepository;
    private final LivroRepository livroRepository;
    private final UserRepository userRepository;
    private final MultaRepository multaRepository;

    EmprestimoService(EmprestimoRepository emprestimoRepository, LivroRepository livroRepository,
                      UserRepository userRepository, MultaRepository multaRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.livroRepository = livroRepository;
        this.userRepository = userRepository;
        this.multaRepository = multaRepository;
    }

    // =========================================================
    // RESERVA
    // =========================================================

    // Reserva feita pelo aluno/professor logado: confere multa, estoque, diminui 1 unidade e registra
    @Transactional
    public EmprestimoEntity reservar(String email, UUID livroId) {

        UserEntity usuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado."));

        LivroEntity livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livro não encontrado."));

        // Pendência financeira bloqueia novas reservas
        if (multaRepository.existsByUsuario_IdAndStatus(usuario.getId(), "pendente")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você possui multa pendente. Pague em Meus Empréstimos para poder reservar.");
        }

        boolean jaTem = emprestimoRepository.existsByUsuario_IdAndLivro_IdAndStatusIn(
                usuario.getId(), livroId, List.of("solicitado", "retirado", "atrasado"));

        if (jaTem) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você já tem uma reserva ou empréstimo ativo deste livro.");
        }

        // Tira 1 do estoque. Se não conseguiu, o livro acabou ou foi desativado.
        int atualizados = livroRepository.reservarUmaUnidade(livroId);

        if (atualizados == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este livro não está mais disponível.");
        }

        int dias = "professor".equalsIgnoreCase(usuario.getRole()) ? 30 : 14;

        EmprestimoEntity novo = new EmprestimoEntity();
        novo.setUsuario(usuario);
        novo.setLivro(livro);
        novo.setDataRetirada(LocalDate.now());
        novo.setDataPrevista(LocalDate.now().plusDays(dias));
        novo.setStatus("solicitado");

        return emprestimoRepository.save(novo);
    }

    // =========================================================
    // DEVOLUÇÃO E EXTRAVIO (bibliotecário)
    // =========================================================

    // Registra a devolução. Devolve 1 ao estoque e, se houve atraso, gera a multa (R$ 2,00/dia).
    // Retorna a multa gerada, ou null se o livro voltou no prazo.
    @Transactional
    public MultaEntity registrarDevolucao(UUID id) {

        EmprestimoEntity emprestimo = buscarEmAndamento(id);

        LocalDate hoje = LocalDate.now();

        emprestimo.setStatus("devolvido");
        emprestimo.setDataDevolucao(hoje);
        emprestimoRepository.save(emprestimo);

        if (emprestimo.getLivro() != null) {
            livroRepository.devolverUmaUnidade(emprestimo.getLivro().getId());
        }

        if (emprestimo.getDataPrevista() != null && hoje.isAfter(emprestimo.getDataPrevista())) {
            long diasAtraso = ChronoUnit.DAYS.between(emprestimo.getDataPrevista(), hoje);

            MultaEntity multa = new MultaEntity();
            multa.setEmprestimo(emprestimo);
            multa.setUsuario(emprestimo.getUsuario());
            multa.setDiasAtraso((int) diasAtraso);
            multa.setValor(VALOR_MULTA_POR_DIA.multiply(BigDecimal.valueOf(diasAtraso)));

            return multaRepository.save(multa);
        }

        return null;
    }

    // Extravio: o livro não volta. O exemplar sai do total do acervo.
    @Transactional
    public void registrarExtravio(UUID id) {

        EmprestimoEntity emprestimo = buscarEmAndamento(id);

        emprestimo.setStatus("extraviado");
        emprestimoRepository.save(emprestimo);

        if (emprestimo.getLivro() != null) {
            livroRepository.baixarUmExemplarDoTotal(emprestimo.getLivro().getId());
        }
    }

    private EmprestimoEntity buscarEmAndamento(UUID id) {
        EmprestimoEntity emprestimo = emprestimoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empréstimo não encontrado."));

        String status = emprestimo.getStatus();

        if (!"retirado".equalsIgnoreCase(status) && !"atrasado".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta operação só vale para livros que estão retirados.");
        }

        return emprestimo;
    }

    // =========================================================
    // RENEGOCIAÇÃO DE PRAZO
    // =========================================================

    // Usuário pede mais prazo (empréstimo quase vencendo ou já vencido)
    @Transactional
    public void solicitarRenegociacao(String email, UUID emprestimoId) {

        EmprestimoEntity emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empréstimo não encontrado."));

        boolean ehDono = emprestimo.getUsuario() != null
                && email.equalsIgnoreCase(emprestimo.getUsuario().getEmail());

        if (!ehDono) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não pode renegociar o empréstimo de outro usuário.");
        }

        String status = emprestimo.getStatus();

        if (!"retirado".equalsIgnoreCase(status) && !"atrasado".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Só é possível renegociar livros que já foram retirados.");
        }

        if (emprestimo.getRenegociacaoStatus() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este empréstimo já teve um pedido de renegociação.");
        }

        long diasParaVencer = ChronoUnit.DAYS.between(LocalDate.now(), emprestimo.getDataPrevista());

        if (diasParaVencer > DIAS_ANTES_PARA_RENEGOCIAR) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A renegociação só pode ser pedida a partir de 3 dias antes do vencimento.");
        }

        emprestimo.setRenegociacaoStatus("pendente");
        emprestimo.setRenegociacaoEm(LocalDate.now());
        emprestimoRepository.save(emprestimo);
    }

    // Bibliotecário aprova ou recusa o pedido. Retorna o novo prazo (ou null se recusou).
    @Transactional
    public LocalDate decidirRenegociacao(UUID emprestimoId, boolean aprovar) {

        EmprestimoEntity emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empréstimo não encontrado."));

        if (!"pendente".equalsIgnoreCase(emprestimo.getRenegociacaoStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não há pedido de renegociação pendente para este empréstimo.");
        }

        if (!aprovar) {
            emprestimo.setRenegociacaoStatus("recusada");
            emprestimoRepository.save(emprestimo);
            return null;
        }

        // O novo prazo conta a partir do vencimento atual, ou de hoje se já venceu
        LocalDate hoje = LocalDate.now();
        LocalDate base = emprestimo.getDataPrevista().isAfter(hoje) ? emprestimo.getDataPrevista() : hoje;
        LocalDate novoPrazo = base.plusDays(DIAS_EXTENSAO_RENEGOCIACAO);

        emprestimo.setDataPrevista(novoPrazo);
        emprestimo.setRenegociacaoStatus("aprovada");

        if ("atrasado".equalsIgnoreCase(emprestimo.getStatus())) {
            emprestimo.setStatus("retirado");
        }

        emprestimoRepository.save(emprestimo);
        return novoPrazo;
    }

    // =========================================================
    // MÉTODOS ANTIGOS
    // =========================================================

    // Realizar uma nova reserva/empréstimo com 14 dias de prazo automático
    public EmprestimoEntity realizarEmprestimo(@NonNull UUID userId, @NonNull UUID livroId) {
        UserEntity usuario = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        LivroEntity livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado."));

        EmprestimoEntity novoEmprestimo = new EmprestimoEntity();
        novoEmprestimo.setUsuario(usuario);
        novoEmprestimo.setLivro(livro);
        novoEmprestimo.setDataRetirada(LocalDate.now());
        novoEmprestimo.setDataPrevista(LocalDate.now().plusDays(14));
        novoEmprestimo.setStatus("ativo");

        return emprestimoRepository.save(novoEmprestimo);
    }

    // Listar empréstimos de um aluno específico
    public List<EmprestimoEntity> listarPorUsuario(UUID userId) {
        return emprestimoRepository.findByUsuarioId(userId);
    }

    // Listar empréstimos para o painel do bibliotecário (ordenados por urgência/vencimento)
    public List<EmprestimoEntity> listarParaPainelBibliotecario() {
        return emprestimoRepository.findAllAtivosOrdenadosPorUrgencia();
    }
}
