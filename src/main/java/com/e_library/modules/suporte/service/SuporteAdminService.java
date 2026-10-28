package com.e_library.modules.suporte.service;

import com.e_library.modules.user.entity.MensagemSuporteEntity;
import com.e_library.modules.user.repository.MensagemSuporteRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SuporteAdminService {

    private final MensagemSuporteRepository mensagemRepository;
    private final AnexoService anexoService;

    public SuporteAdminService(MensagemSuporteRepository mensagemRepository, AnexoService anexoService) {
        this.mensagemRepository = mensagemRepository;
        this.anexoService = anexoService;
    }

    // Mensagens do chat enviadas ao bibliotecário, com quem enviou e os anexos
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarChat(String status) {

        List<MensagemSuporteEntity> mensagens = (status == null || status.isBlank())
                ? mensagemRepository.findAllByOrderByCriadoEmDesc()
                : mensagemRepository.findByStatusIgnoreCaseOrderByCriadoEmDesc(status);

        List<Map<String, Object>> lista = new ArrayList<>();

        for (MensagemSuporteEntity m : mensagens) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("nome", m.getUsuario() != null ? m.getUsuario().getName() : null);
            item.put("email", m.getUsuario() != null ? m.getUsuario().getEmail() : null);
            item.put("mensagem", m.getMensagem());
            item.put("status", m.getStatus());
            item.put("criadoEm", m.getCriadoEm());
            item.put("anexos", anexoService.listarDaMensagem(m.getId()));
            lista.add(item);
        }

        return lista;
    }

    @Transactional
    public boolean resolver(UUID id) {
        MensagemSuporteEntity mensagem = mensagemRepository.findById(id).orElse(null);

        if (mensagem == null) {
            return false;
        }

        mensagem.setStatus("resolvido");
        mensagemRepository.save(mensagem);
        return true;
    }
}
