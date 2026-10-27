package com.e_library.modules.livro.controller;

import com.e_library.modules.livro.entity.CategoriaEntity;
import com.e_library.modules.livro.repository.CategoriaRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(origins = "*")
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;

    public CategoriaController(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    // Lista as categorias ativas em ordem alfabética (qualquer usuário logado)
    @GetMapping
    public ResponseEntity<List<CategoriaEntity>> listar() {
        return ResponseEntity.ok(categoriaRepository.findByAtivoTrueOrderByNomeAsc());
    }
}
