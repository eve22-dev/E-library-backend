package com.e_library.modules.multa.entity;

import com.e_library.modules.emprestimo.entity.EmprestimoEntity;
import com.e_library.modules.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "multas")
public class MultaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "emprestimo_id")
    private EmprestimoEntity emprestimo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private UserEntity usuario;

    @Column(name = "dias_atraso")
    private Integer diasAtraso;

    @Column(name = "valor", precision = 10, scale = 2)
    private BigDecimal valor;

    // pendente ou paga
    private String status = "pendente";

    @Column(name = "criada_em")
    private LocalDate criadaEm = LocalDate.now();

    @Column(name = "paga_em")
    private LocalDate pagaEm;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public EmprestimoEntity getEmprestimo() { return emprestimo; }
    public void setEmprestimo(EmprestimoEntity emprestimo) { this.emprestimo = emprestimo; }

    public UserEntity getUsuario() { return usuario; }
    public void setUsuario(UserEntity usuario) { this.usuario = usuario; }

    public Integer getDiasAtraso() { return diasAtraso; }
    public void setDiasAtraso(Integer diasAtraso) { this.diasAtraso = diasAtraso; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getCriadaEm() { return criadaEm; }
    public void setCriadaEm(LocalDate criadaEm) { this.criadaEm = criadaEm; }

    public LocalDate getPagaEm() { return pagaEm; }
    public void setPagaEm(LocalDate pagaEm) { this.pagaEm = pagaEm; }
}
