package com.e_library.modules.multa.repository;

import com.e_library.modules.multa.entity.MultaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MultaRepository extends JpaRepository<MultaEntity, UUID> {

    List<MultaEntity> findByUsuario_IdOrderByCriadaEmDesc(UUID usuarioId);

    List<MultaEntity> findAllByOrderByCriadaEmDesc();

    boolean existsByUsuario_IdAndStatus(UUID usuarioId, String status);
}
