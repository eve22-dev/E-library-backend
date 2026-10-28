package com.e_library.modules.user.repository;

import com.e_library.modules.user.entity.MensagemSuporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MensagemSuporteRepository extends JpaRepository<MensagemSuporteEntity, UUID> {

    List<MensagemSuporteEntity> findAllByOrderByCriadoEmDesc();

    List<MensagemSuporteEntity> findByStatusIgnoreCaseOrderByCriadoEmDesc(String status);
}
