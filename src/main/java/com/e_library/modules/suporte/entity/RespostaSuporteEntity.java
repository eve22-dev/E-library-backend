package com.e_library.modules.suporte.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

// Uma mensagem dentro da conversa do chat (do aluno ou do bibliotecário)
@Entity
@Table(name = "respostas_suporte")
public class RespostaSuporteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mensagem_id", nullable = false)
    private UUID mensagemId;

    // aluno ou bibliotecario
    @Column(nullable = false)
    private String autor;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String texto;

    // vale para respostas do bibliotecário: o aluno já viu?
    @Column(nullable = false)
    private boolean lida = false;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        criadoEm = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMensagemId() { return mensagemId; }
    public void setMensagemId(UUID mensagemId) { this.mensagemId = mensagemId; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public boolean isLida() { return lida; }
    public void setLida(boolean lida) { this.lida = lida; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
