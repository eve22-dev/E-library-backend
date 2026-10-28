package com.e_library.modules.suporte.repository;

import com.e_library.modules.suporte.entity.SolicitacaoSuporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SolicitacaoSuporteRepository extends JpaRepository<SolicitacaoSuporteEntity, UUID> {

    List<SolicitacaoSuporteEntity> findAllByOrderByCriadoEmDesc();

    List<SolicitacaoSuporteEntity> findByStatusIgnoreCaseOrderByCriadoEmDesc(String status);
}
