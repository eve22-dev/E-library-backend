package com.e_library.modules.livro.repository;

import com.e_library.modules.livro.entity.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<CategoriaEntity, UUID> {

    // Só as categorias ativas (as excluídas ficam no banco com o motivo)
    List<CategoriaEntity> findByAtivoTrueOrderByNomeAsc();
}
