package com.e_library.modules.livro.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "categorias")
public class CategoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    // Construtor vazio obrigatório para o JPA
    public CategoriaEntity() {}

    // Getters e Setters
    public UUID getId() { 
        return id; 
    }
    
    public void setId(UUID id) { 
        this.id = id; 
    }

    public String getNome() { 
        return nome; 
    }
    
    public void setNome(String nome) { 
        this.nome = nome; 
    }

    public String getDescricao() { 
        return descricao; 
    }
    
    public void setDescricao(String descricao) { 
        this.descricao = descricao; 
    }

    @Column(name = "ativo")
    private Boolean ativo = true;

    @Column(name = "motivo_inatividade", columnDefinition = "TEXT")
    private String motivoInatividade;

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public String getMotivoInatividade() { return motivoInatividade; }
    public void setMotivoInatividade(String motivoInatividade) { this.motivoInatividade = motivoInatividade; }
}