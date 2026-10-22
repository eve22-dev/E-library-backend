package com.e_library.modules.exemplar.entity;

import com.e_library.modules.livro.entity.LivroEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma cópia física de um livro. O ISBN identifica a obra (tabela livros);
 * o código do exemplar identifica ESTA cópia (tombo/etiqueta da biblioteca).
 * Status: disponivel, emprestado, extraviado ou baixado.
 */
@Entity
@Table(name = "exemplares")
public class ExemplarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "livro_id", nullable = false)
    private LivroEntity livro;

    @Column(name = "codigo", nullable = false, unique = true, length = 60)
    private String codigo;

    @Column(name = "status", nullable = false)
    private String status = "disponivel";

    @Column(name = "criado_em")
    private LocalDate criadoEm = LocalDate.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LivroEntity getLivro() { return livro; }
    public void setLivro(LivroEntity livro) { this.livro = livro; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDate criadoEm) { this.criadoEm = criadoEm; }
}
