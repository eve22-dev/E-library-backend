package com.e_library.modules.user.service;

import com.e_library.modules.user.entity.MensagemSuporteEntity;
import com.e_library.modules.user.entity.UserEntity;
import com.e_library.modules.user.repository.MensagemSuporteRepository;
import com.e_library.modules.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SuporteService {

    private final MensagemSuporteRepository suporteRepository;
    private final UserRepository userRepository;

    public SuporteService(MensagemSuporteRepository suporteRepository, UserRepository userRepository) {
        this.suporteRepository = suporteRepository;
        this.userRepository = userRepository;
    }

    // Registra a dúvida do usuário LOGADO para o bibliotecário
    public void salvarMensagemSuporte(String emailDoUsuario, String mensagem) {

        if (mensagem == null || mensagem.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escreva a sua dúvida.");
        }

        UserEntity usuario = userRepository.findByEmail(emailDoUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado."));

        MensagemSuporteEntity ticket = new MensagemSuporteEntity();
        ticket.setUsuario(usuario);
        ticket.setMensagem(mensagem.trim());
        ticket.setStatus("pendente");

        suporteRepository.save(ticket);
    }
}
