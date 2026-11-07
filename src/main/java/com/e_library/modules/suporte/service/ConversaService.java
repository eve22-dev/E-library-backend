package com.e_library.modules.suporte.service;

import com.e_library.modules.suporte.entity.RespostaSuporteEntity;
import com.e_library.modules.suporte.repository.RespostaSuporteRepository;
import com.e_library.modules.user.entity.MensagemSuporteEntity;
import com.e_library.modules.user.repository.MensagemSuporteRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Conversa do chat entre o aluno e o bibliotecário, tudo dentro da plataforma.
 * Situação da conversa: pendente (aguardando o bibliotecário), respondido (aguardando o aluno) ou resolvido.
 */
@Service
public class ConversaService {

    private final MensagemSuporteRepository mensagemRepository;
    private final RespostaSuporteRepository respostaRepository;
    private final AnexoService anexoService;

    public ConversaService(MensagemSuporteRepository mensagemRepository,
                           RespostaSuporteRepository respostaRepository,
                           AnexoService anexoService) {
        this.mensagemRepository = mensagemRepository;
        this.respostaRepository = respostaRepository;
        this.anexoService = anexoService;
    }

    // ---------------- lado do aluno ----------------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> conversasDoUsuario(String email) {
        List<Map<String, Object>> lista = new ArrayList<>();

        for (MensagemSuporteEntity m : mensagemRepository.findByUsuario_EmailOrderByCriadoEmDesc(email)) {
            lista.add(montar(m, false));
        }

        return lista;
    }

    @Transactional
    public void responderComoAluno(String email, UUID mensagemId, String texto, List<AnexoService.ArquivoValidado> anexos) {

        MensagemSuporteEntity mensagem = buscar(mensagemId);

        if (mensagem.getUsuario() == null || !email.equalsIgnoreCase(mensagem.getUsuario().getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta conversa não é sua.");
        }

        if ("resolvido".equalsIgnoreCase(mensagem.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta conversa foi encerrada. Envie uma nova mensagem para abrir outra.");
        }

        RespostaSuporteEntity resposta = novaResposta(mensagemId, "aluno", texto, true);
        respostaRepository.saveAndFlush(resposta);
        anexoService.salvar(mensagemId, resposta.getId(), anexos);

        // volta a aguardar o bibliotecário
        mensagem.setStatus("pendente");
        mensagemRepository.save(mensagem);
    }

    // O aluno abriu a conversa: as respostas do bibliotecário passam a contar como lidas
    @Transactional
    public void marcarLidas(String email, UUID mensagemId) {
        MensagemSuporteEntity mensagem = buscar(mensagemId);

        if (mensagem.getUsuario() == null || !email.equalsIgnoreCase(mensagem.getUsuario().getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta conversa não é sua.");
        }

        for (RespostaSuporteEntity r : respostaRepository.findByMensagemIdOrderByCriadoEmAsc(mensagemId)) {
            if ("bibliotecario".equals(r.getAutor()) && !r.isLida()) {
                r.setLida(true);
                respostaRepository.save(r);
            }
        }
    }

    // ---------------- lado do bibliotecário ----------------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarChat(String status) {
        List<MensagemSuporteEntity> mensagens = (status == null || status.isBlank())
                ? mensagemRepository.findAllByOrderByCriadoEmDesc()
                : mensagemRepository.findByStatusIgnoreCaseOrderByCriadoEmDesc(status);

        List<Map<String, Object>> lista = new ArrayList<>();

        for (MensagemSuporteEntity m : mensagens) {
            lista.add(montar(m, true));
        }

        return lista;
    }

    @Transactional
    public void responderComoBibliotecario(UUID mensagemId, String texto) {
        MensagemSuporteEntity mensagem = buscar(mensagemId);

        if ("resolvido".equalsIgnoreCase(mensagem.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta conversa já foi resolvida.");
        }

        respostaRepository.save(novaResposta(mensagemId, "bibliotecario", texto, false));

        mensagem.setStatus("respondido");
        mensagemRepository.save(mensagem);
    }

    @Transactional
    public boolean resolver(UUID mensagemId) {
        MensagemSuporteEntity mensagem = mensagemRepository.findById(mensagemId).orElse(null);

        if (mensagem == null) {
            return false;
        }

        mensagem.setStatus("resolvido");
        mensagemRepository.save(mensagem);
        return true;
    }

    // O aluno só baixa anexos das próprias conversas
    @Transactional(readOnly = true)
    public boolean podeBaixarAnexo(String email, UUID anexoId) {
        return anexoService.mensagemDoAnexo(anexoId)
                .flatMap(mensagemRepository::findById)
                .map(m -> m.getUsuario() != null && email.equalsIgnoreCase(m.getUsuario().getEmail()))
                .orElse(false);
    }

    // ---------------- apoio ----------------

    private MensagemSuporteEntity buscar(UUID id) {
        return mensagemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversa não encontrada."));
    }

    private RespostaSuporteEntity novaResposta(UUID mensagemId, String autor, String texto, boolean lida) {
        String limpo = texto == null ? "" : texto.trim();

        if (limpo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escreva a mensagem.");
        }

        if (limpo.length() > 3000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A mensagem pode ter no máximo 3000 caracteres.");
        }

        RespostaSuporteEntity r = new RespostaSuporteEntity();
        r.setMensagemId(mensagemId);
        r.setAutor(autor);
        r.setTexto(limpo);
        r.setLida(lida);
        return r;
    }

    private Map<String, Object> montar(MensagemSuporteEntity m, boolean comUsuario) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", m.getId());

        if (comUsuario) {
            item.put("nome", m.getUsuario() != null ? m.getUsuario().getName() : null);
            item.put("email", m.getUsuario() != null ? m.getUsuario().getEmail() : null);
        }

        item.put("mensagem", m.getMensagem());
        item.put("status", m.getStatus());
        item.put("criadoEm", m.getCriadoEm());
        item.put("anexos", anexoService.listarDaMensagem(m.getId()));

        List<Map<String, Object>> respostas = new ArrayList<>();
        int naoLidas = 0;

        for (RespostaSuporteEntity r : respostaRepository.findByMensagemIdOrderByCriadoEmAsc(m.getId())) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", r.getId());
            resp.put("autor", r.getAutor());
            resp.put("texto", r.getTexto());
            resp.put("criadoEm", r.getCriadoEm());
            resp.put("anexos", anexoService.listarDaResposta(r.getId()));
            respostas.add(resp);

            if ("bibliotecario".equals(r.getAutor()) && !r.isLida()) {
                naoLidas++;
            }
        }

        item.put("respostas", respostas);
        item.put("naoLidas", naoLidas);
        return item;
    }
}
