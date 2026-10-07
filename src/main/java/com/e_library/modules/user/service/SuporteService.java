package com.e_library.modules.user.service;

import com.e_library.modules.user.entity.MensagemSuporteEntity;
import com.e_library.modules.user.entity.UserEntity;
import com.e_library.modules.user.repository.MensagemSuporteRepository;
import com.e_library.modules.user.repository.UserRepository;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SuporteService {

    private final MensagemSuporteRepository suporteRepository = null;

    private final UserRepository userRepository = null;

    public void salvarMensagemSuporte(@NonNull UUID userId, String mensagem) {
        // 1. Busca o usuário logado no banco de dados usando o UUID
        UserEntity usuario = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        // 2. Monta a nova solicitação de atendimento
        MensagemSuporteEntity ticket = new MensagemSuporteEntity();
        ticket.setUsuario(usuario);
        ticket.setMensagem(mensagem);
        ticket.setStatus("Pendente"); // Status inicial para o bibliotecário avaliar

        // 3. Salva no banco de dados
        suporteRepository.save(ticket);
    }
}