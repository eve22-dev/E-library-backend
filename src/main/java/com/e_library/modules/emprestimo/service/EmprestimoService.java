package com.e_library.modules.emprestimo.service;

import com.e_library.modules.emprestimo.entity.EmprestimoEntity;
import com.e_library.modules.emprestimo.repository.EmprestimoRepository;
import com.e_library.modules.exemplar.entity.ExemplarEntity;
import com.e_library.modules.exemplar.repository.ExemplarRepository;
import com.e_library.modules.exemplar.service.ExemplarService;
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

    // Depois de um extravio, o usuário tem 30 dias para entregar um exemplar novo
    private static final int PRAZO_REPOSICAO_DIAS = 30;

    private final EmprestimoRepository emprestimoRepository;
    private final LivroRepository livroRepository;
    private final UserRepository userRepository;
    private final MultaRepository multaRepository;
    private final ExemplarRepository exemplarRepository;

    EmprestimoService(EmprestimoRepository emprestimoRepository, LivroRepository livroRepository,
                      UserRepository userRepository, MultaRepository multaRepository,
                      ExemplarRepository exemplarRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.livroRepository = livroRepository;
        this.userRepository = userRepository;
        this.multaRepository = multaRepository;
        this.exemplarRepository = exemplarRepository;
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
    // ENTREGA, DEVOLUÇÃO, EXTRAVIO E REPOSIÇÃO (bibliotecário)
    // =========================================================

    // Entrega: o bibliotecário bipa o código da cópia física que está saindo.
    // O sistema registra qual exemplar ficou com o usuário (rastreabilidade).
    @Transactional
    public ExemplarEntity entregar(UUID id, String codigoExemplar) {

        EmprestimoEntity emprestimo = emprestimoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empréstimo não encontrado."));

        if (!"solicitado".equalsIgnoreCase(emprestimo.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Só reservas aguardando retirada podem ser entregues.");
        }

        String codigo = ExemplarService.normalizarCodigo(codigoExemplar);

        if (codigo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o código do exemplar que está sendo entregue.");
        }

        ExemplarEntity exemplar = exemplarRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Exemplar não encontrado para o código " + codigo + "."));

        if (!exemplar.getLivro().getId().equals(emprestimo.getLivro().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este exemplar pertence a outro livro (" + exemplar.getLivro().getTitulo() + ").");
        }

        if (!"disponivel".equalsIgnoreCase(exemplar.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este exemplar não está disponível (situação: " + exemplar.getStatus() + ").");
        }

        exemplar.setStatus("emprestado");
        exemplarRepository.save(exemplar);

        // O estoque já foi reduzido na reserva; aqui só registramos qual cópia saiu
        emprestimo.setExemplar(exemplar);
        emprestimo.setStatus("retirado");
        emprestimoRepository.save(emprestimo);

        return exemplar;
    }

    // Registra a devolução. A cópia volta a ficar disponível e, se houve atraso, gera a multa (R$ 2,00/dia).
    // Retorna a multa gerada, ou null se o livro voltou no prazo.
    @Transactional
    public MultaEntity registrarDevolucao(UUID id) {

        EmprestimoEntity emprestimo = buscarEmAndamento(id);

        LocalDate hoje = LocalDate.now();

        emprestimo.setStatus("devolvido");
        emprestimo.setDataDevolucao(hoje);
        emprestimoRepository.save(emprestimo);

        if (emprestimo.getExemplar() != null) {
            ExemplarEntity exemplar = emprestimo.getExemplar();
            exemplar.setStatus("disponivel");
            exemplarRepository.save(exemplar);
        }

        if (emprestimo.getLivro() != null) {
            livroRepository.devolverUmaUnidade(emprestimo.getLivro().getId());
        }

        return gerarMultaSeAtrasado(emprestimo, hoje);
    }

    // Extravio: o livro não volta. O atraso congela na data do aviso (multa dos dias já atrasados)
    // e abre um prazo de 30 dias para o usuário entregar um exemplar novo.
    @Transactional
    public MultaEntity registrarExtravio(UUID id) {

        EmprestimoEntity emprestimo = buscarEmAndamento(id);

        LocalDate hoje = LocalDate.now();

        emprestimo.setStatus("extraviado");
        emprestimo.setReposicaoAte(hoje.plusDays(PRAZO_REPOSICAO_DIAS));
        emprestimoRepository.save(emprestimo);

        if (emprestimo.getExemplar() != null) {
            ExemplarEntity exemplar = emprestimo.getExemplar();
            exemplar.setStatus("extraviado");
            exemplarRepository.save(exemplar);
        }

        return gerarMultaSeAtrasado(emprestimo, hoje);
    }

    // Reposição: o usuário entregou um exemplar novo (com código próprio). Fecha a ocorrência.
    @Transactional
    public void registrarReposicao(UUID id, String codigoNovoExemplar) {

        EmprestimoEntity emprestimo = buscarExtraviado(id);

        String codigo = ExemplarService.normalizarCodigo(codigoNovoExemplar);

        if (codigo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o código do exemplar novo que foi entregue.");
        }

        if (exemplarRepository.existsByCodigo(codigo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O código " + codigo + " já está cadastrado em outro exemplar.");
        }

        ExemplarEntity novo = new ExemplarEntity();
        novo.setLivro(emprestimo.getLivro());
        novo.setCodigo(codigo);
        novo.setStatus("disponivel");
        exemplarRepository.save(novo);

        if (emprestimo.getExemplar() != null) {
            ExemplarEntity perdido = emprestimo.getExemplar();
            perdido.setStatus("baixado");
            exemplarRepository.save(perdido);
        }

        emprestimo.setStatus("reposto");
        emprestimo.setDataDevolucao(LocalDate.now());
        emprestimoRepository.save(emprestimo);

        // O total do acervo não muda (a cópia nova substitui a perdida); o disponível sobe 1
        livroRepository.devolverUmaUnidade(emprestimo.getLivro().getId());
    }

    // Baixa definitiva: o livro não foi reposto. O exemplar sai do total do acervo.
    @Transactional
    public void registrarBaixa(UUID id) {

        EmprestimoEntity emprestimo = buscarExtraviado(id);

        if (emprestimo.getExemplar() != null) {
            ExemplarEntity perdido = emprestimo.getExemplar();
            perdido.setStatus("baixado");
            exemplarRepository.save(perdido);
        }

        emprestimo.setStatus("baixado");
        emprestimoRepository.save(emprestimo);

        livroRepository.baixarUmExemplarDoTotal(emprestimo.getLivro().getId());
    }

    private MultaEntity gerarMultaSeAtrasado(EmprestimoEntity emprestimo, LocalDate hoje) {
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

    private EmprestimoEntity buscarExtraviado(UUID id) {
        EmprestimoEntity emprestimo = emprestimoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empréstimo não encontrado."));

        if (!"extraviado".equalsIgnoreCase(emprestimo.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta operação só vale para livros extraviados aguardando reposição.");
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
