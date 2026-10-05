package com.e_library.modules.emprestimo.entity;

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

@Entity
@Table(name = "emprestimos")
public class EmprestimoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "livro_id")
    private LivroEntity livro;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "data_retirada")
    private LocalDate dataRetirada;

    @Column(name = "data_prevista")
    private LocalDate dataPrevista;

    private String status;

    // --- Getters e Setters Limpos ---

    public UUID getId() { 
        return id; 
    }
    
    public void setId(UUID id) { 
        this.id = id; 
    }

    public LivroEntity getLivro() { 
        return livro; 
    }
    
    public void setLivro(LivroEntity livro) { 
        this.livro = livro; 
    }

    public UUID getUserId() { 
        return userId; 
    }
    
    public void setUserId(UUID userId) { 
        this.userId = userId; 
    }

    public LocalDate getDataRetirada() { 
        return dataRetirada; 
    }
    
    public void setDataRetirada(LocalDate dataRetirada) { 
        this.dataRetirada = dataRetirada; 
    }

    public LocalDate getDataPrevista() { 
        return dataPrevista; 
    }
    
    public void setDataPrevista(LocalDate dataPrevista) { 
        this.dataPrevista = dataPrevista; 
    }

    public String getStatus() { 
        return status; 
    }
    
    public void setStatus(String status) { 
        this.status = status; 
    }
}