package com.e_library.modules.livro.repository;

import com.e_library.modules.livro.entity.LivroEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LivroRepository extends JpaRepository<LivroEntity, UUID> {

    List<LivroEntity> findByTituloContainingIgnoreCaseAndAtivoTrue(String titulo);
    
    List<LivroEntity> findByAutorContainingIgnoreCaseAndAtivoTrue(String autor);
    
    List<LivroEntity> findByIsbnContainingIgnoreCaseAndAtivoTrue(String isbn);
}