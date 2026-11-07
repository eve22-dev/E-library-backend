package com.e_library.modules.suporte.repository;

import com.e_library.modules.suporte.entity.RespostaSuporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RespostaSuporteRepository extends JpaRepository<RespostaSuporteEntity, UUID> {

    List<RespostaSuporteEntity> findByMensagemIdOrderByCriadoEmAsc(UUID mensagemId);
}
