package com.e_library.modules.exemplar.service;

import com.e_library.modules.exemplar.entity.ExemplarEntity;
import com.e_library.modules.exemplar.repository.ExemplarRepository;
import com.e_library.modules.livro.entity.CategoriaEntity;
import com.e_library.modules.livro.entity.LivroEntity;
import com.e_library.modules.livro.repository.CategoriaRepository;
import com.e_library.modules.livro.repository.LivroRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ExemplarService {

    public record LivroDados(String titulo, String autor, String editora, String isbn, UUID categoriaId) {}

    public record CadastroRequest(UUID livroId, LivroDados livro, List<String> codigos) {}

    private final ExemplarRepository exemplarRepository;
    private final LivroRepository livroRepository;
    private final CategoriaRepository categoriaRepository;

    public ExemplarService(ExemplarRepository exemplarRepository, LivroRepository livroRepository,
                           CategoriaRepository categoriaRepository) {
        this.exemplarRepository = exemplarRepository;
        this.livroRepository = livroRepository;
        this.categoriaRepository = categoriaRepository;
    }

    // Padroniza o código lido pelo scanner ou digitado: sem espaços e em maiúsculas
    public static String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase();
    }

    public static String normalizarIsbn(String isbn) {
        return isbn == null ? "" : isbn.replace("-", "").replace(" ", "").trim();
    }

    // Cadastro de um ou mais exemplares. Cria o livro se ele ainda não existe (por ISBN).
    @Transactional
    public Map<String, Object> cadastrar(CadastroRequest req) {

        if (req == null || req.codigos() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe ao menos um código de exemplar.");
        }

        List<String> codigos = new ArrayList<>();
        for (String c : req.codigos()) {
            String n = normalizarCodigo(c);
            if (!n.isEmpty()) codigos.add(n);
        }

        if (codigos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe ao menos um código de exemplar.");
        }

        Set<String> unicos = new LinkedHashSet<>(codigos);
        if (unicos.size() != codigos.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Há códigos repetidos na lista.");
        }

        for (String c : codigos) {
            if (c.length() > 60) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O código " + c + " é grande demais (máximo 60 caracteres).");
            }
            if (exemplarRepository.existsByCodigo(c)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "O código " + c + " já está cadastrado em outro exemplar.");
            }
        }

        LivroEntity livro = null;
        boolean livroNovo = false;

        if (req.livroId() != null) {
            livro = livroRepository.findById(req.livroId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livro não encontrado."));

            if (!Boolean.TRUE.equals(livro.getAtivo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este livro está desativado.");
            }
        } else {
            LivroDados dados = req.livro();

            if (dados == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os dados do livro.");
            }

            String isbn = normalizarIsbn(dados.isbn());

            // Mesmo ISBN = mesma obra: só acrescenta exemplares
            if (!isbn.isEmpty()) {
                List<LivroEntity> existentes = livroRepository.buscarPorIsbnNormalizado(isbn);
                if (!existentes.isEmpty()) {
                    livro = existentes.get(0);
                }
            }

            if (livro == null) {
                if (vazio(dados.titulo()) || vazio(dados.autor()) || dados.categoriaId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título, autor e categoria são obrigatórios.");
                }

                CategoriaEntity categoria = categoriaRepository.findById(dados.categoriaId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada."));

                livro = new LivroEntity();
                livro.setTitulo(dados.titulo().trim());
                livro.setAutor(dados.autor().trim());
                livro.setEditora(vazio(dados.editora()) ? null : dados.editora().trim());
                livro.setIsbn(isbn.isEmpty() ? null : isbn);
                livro.setCategoria(categoria);
                livro.setAtivo(true);
                livro.setQtdTotal(0);
                livro.setQtdDisponivel(0);
                livro = livroRepository.save(livro);
                livroNovo = true;
            }
        }

        for (String c : codigos) {
            ExemplarEntity ex = new ExemplarEntity();
            ex.setLivro(livro);
            ex.setCodigo(c);
            ex.setStatus("disponivel");
            exemplarRepository.save(ex);
        }

        // Atualiza o estoque de uma vez só
        livroRepository.acrescentarExemplares(livro.getId(), codigos.size());

        LivroEntity atualizado = livroRepository.findById(livro.getId()).orElse(livro);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("livroId", atualizado.getId());
        resposta.put("titulo", atualizado.getTitulo());
        resposta.put("livroNovo", livroNovo);
        resposta.put("codigosCadastrados", codigos);
        resposta.put("qtdTotal", atualizado.getQtdTotal());
        resposta.put("qtdDisponivel", atualizado.getQtdDisponivel());
        return resposta;
    }

    private boolean vazio(String s) {
        return s == null || s.isBlank();
    }
}
