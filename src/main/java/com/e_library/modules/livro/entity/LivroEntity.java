package com.e_library.modules.livro.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "livros") 
public class LivroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) 
    private UUID id;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "autor", nullable = false)
    private String autor; 

    @Column(name = "isbn")
    private String isbn; 

    @Column(name = "qtd_total")
    private Integer qtdTotal; 

    @Column(name = "qtd_disponivel")
    private Integer qtdDisponivel; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaEntity categoria;

    @Column(name = "ativo")
    private Boolean ativo = true;

    @Column(name = "motivo_inatividade", columnDefinition = "TEXT")
    private String motivoInatividade;

    @Column(name = "editora")
    private String editora;

    public LivroEntity() {}

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

    public Integer getQtdDisponivel() { return qtdDisponivel; }
    public void setQtdDisponivel(Integer qtdDisponivel) { this.qtdDisponivel = qtdDisponivel; }

    public CategoriaEntity getCategoria() { return categoria; }
    public void setCategoria(CategoriaEntity categoria) { this.categoria = categoria; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public String getMotivoInatividade() { return motivoInatividade; }
    public void setMotivoInatividade(String motivoInatividade) { this.motivoInatividade = motivoInatividade; }

    public String getEditora() { return editora; }
    public void setEditora(String editora) { this.editora = editora; }
}