package com.e_library.modules.livro.repository;

import com.e_library.modules.livro.entity.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<CategoriaEntity, UUID> {
}