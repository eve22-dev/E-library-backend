package com.e_library.modules.livro.repository;

import com.e_library.modules.livro.entity.LivroEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LivroRepository extends JpaRepository<LivroEntity, UUID> {
    
    // Métodos mágicos do Spring Data JPA para o campo de pesquisa com filtros.
    // O "ContainingIgnoreCase" faz a busca como um "LIKE %termo%" no banco, ignorando maiúsculas e minúsculas.
    // O "AndAtivoTrue" garante que não traga livros que o bibliotecário desativou.
    
    List<LivroEntity> findByTituloContainingIgnoreCaseAndAtivoTrue(String titulo);
    
    List<LivroEntity> findByAutorContainingIgnoreCaseAndAtivoTrue(String autor);
    
    List<LivroEntity> findByIsbnContainingIgnoreCaseAndAtivoTrue(String isbn);
}