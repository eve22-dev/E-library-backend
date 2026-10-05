package com.e_library.modules.emprestimo.dto;

import com.e_library.modules.emprestimo.entity.EmprestimoEntity;
import java.time.LocalDate;
import java.util.UUID;
import com.e_library.modules.livro.entity.LivroEntity;

@SuppressWarnings("unused")
public class EmprestimoResponseDTO {
    
    private UUID id;
    private UUID livroId;
    private String tituloLivro;
    private LocalDate dataRetirada;
    private LocalDate dataPrevista;
    private String status;

    // Construtor que recebe a Entidade e converte para este DTO
    public EmprestimoResponseDTO(EmprestimoEntity entity) {
        this.id = entity.getId();
        this.dataRetirada = entity.getDataRetirada();
        this.dataPrevista = entity.getDataPrevista();
        this.status = entity.getStatus();
        
        // Acede ao relacionamento @ManyToOne de forma segura
        if (entity.getLivro() != null) {
            this.livroId = entity.getLivro().getId();
            // AQUI FOI CORRIGIDO: Usamos getNome() conforme definido no seu LivroEntity
            this.tituloLivro = entity.getLivro().getNome(); 
        }
    }

    // --- Getters e Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getLivroId() { return livroId; }
    public void setLivroId(UUID livroId) { this.livroId = livroId; }

    public String getTituloLivro() { return tituloLivro; }
    public void setTituloLivro(String tituloLivro) { this.tituloLivro = tituloLivro; }

    public LocalDate getDataRetirada() { return dataRetirada; }
    public void setDataRetirada(LocalDate dataRetirada) { this.dataRetirada = dataRetirada; }

    public LocalDate getDataPrevista() { return dataPrevista; }
    public void setDataPrevista(LocalDate dataPrevista) { this.dataPrevista = dataPrevista; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

}