package com.e_library.modules.livro.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "livros") // Alterado de "livro" para "livros"
public class LivroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "titulo", nullable = false) // Alterado de nome para titulo
    private String titulo;

    @Column(name = "autor", nullable = false)
    private String autor; // Adicionado do diagrama

    @Column(name = "isbn")
    private String isbn; // Adicionado do diagrama

    @Column(name = "qtd_total")
    private Integer qtdTotal; // Adicionado do diagrama

    @Column(name = "qtd_disponivel")
    private Integer qtdDisponivel; // Adicionado do diagrama

    // RELACIONAMENTO COM A CATEGORIA
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaEntity categoria;

    // CAMPOS PARA A REGRA DO BIBLIOTECÁRIO CANCELAR/DESATIVAR
    @Column(name = "ativo")
    private Boolean ativo = true;

    @Column(name = "motivo_inatividade", columnDefinition = "TEXT")
    private String motivoInatividade;

    // Construtor vazio obrigatório para o JPA
    public LivroEntity() {}

    // --- GETTERS E SETTERS ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public Integer getQtdTotal() { return qtdTotal; }
    public void setQtdTotal(Integer qtdTotal) { this.qtdTotal = qtdTotal; }

    // --- ADICIONADO O QUE FALTAVA ABAIXO ---

    public Integer getQtdDisponivel() { return qtdDisponivel; }
    public void setQtdDisponivel(Integer qtdDisponivel) { this.qtdDisponivel = qtdDisponivel; }

    public CategoriaEntity getCategoria() { return categoria; }
    public void setCategoria(CategoriaEntity categoria) { this.categoria = categoria; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public String getMotivoInatividade() { return motivoInatividade; }
    public void setMotivoInatividade(String motivoInatividade) { this.motivoInatividade = motivoInatividade; }

} // Chave que faltava para fechar a classe