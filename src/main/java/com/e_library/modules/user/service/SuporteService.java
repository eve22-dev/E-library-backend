package com.e_library.modules.user.service;

import com.e_library.modules.suporte.service.AnexoService;
import com.e_library.modules.user.entity.MensagemSuporteEntity;
import com.e_library.modules.user.entity.UserEntity;
import com.e_library.modules.user.repository.MensagemSuporteRepository;
import com.e_library.modules.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SuporteService {

    private final MensagemSuporteRepository suporteRepository;
    private final UserRepository userRepository;
    private final AnexoService anexoService;

    public SuporteService(MensagemSuporteRepository suporteRepository, UserRepository userRepository,
                          AnexoService anexoService) {
        this.suporteRepository = suporteRepository;
        this.userRepository = userRepository;
        this.anexoService = anexoService;
    }

    // Registra a dúvida do usuário LOGADO (com anexos) para o bibliotecário
    @Transactional
    public void salvarMensagemSuporte(String emailDoUsuario, String mensagem, List<AnexoService.ArquivoValidado> anexos) {

        if (mensagem == null || mensagem.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escreva a sua dúvida.");
        }

        if (mensagem.length() > 3000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A mensagem pode ter no máximo 3000 caracteres.");
        }

        UserEntity usuario = userRepository.findByEmail(emailDoUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado."));

        MensagemSuporteEntity ticket = new MensagemSuporteEntity();
        ticket.setUsuario(usuario);
        ticket.setMensagem(mensagem.trim());
        ticket.setStatus("pendente");

        // saveAndFlush: a mensagem precisa existir no banco antes de gravar os anexos ligados a ela
        suporteRepository.saveAndFlush(ticket);

        anexoService.salvar(ticket.getId(), anexos);
    }
}
