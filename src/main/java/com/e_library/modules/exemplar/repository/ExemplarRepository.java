package com.e_library.modules.exemplar.repository;

import com.e_library.modules.exemplar.entity.ExemplarEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExemplarRepository extends JpaRepository<ExemplarEntity, UUID> {

    Optional<ExemplarEntity> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<ExemplarEntity> findByLivro_IdOrderByCodigoAsc(UUID livroId);
}
