package com.e_library.modules.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import com.e_library.enums.Role;

@Entity
@Table(name = "usuarios") // Alterado para bater com o banco
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // Alterado para suportar UUID
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    // Vou manter a senha aqui caso você faça a validação internamente, 
    // mas lembre-se que seu banco liga com auth.users do Supabase.
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel") // Mapeando para a coluna 'papel' no banco
    private Role role = Role.USER;

    @Column(name = "ativo")
    private Boolean active = true;

    @Column(name = "criado_em")
    private LocalDateTime createdAt;

    // Faltavam estes campos do seu diagrama
    @Column(name = "rgm")
    private String rgm;

    @Column(name = "curso")
    private String curso;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    // --- Getters e Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getRgm() { return rgm; }
    public void setRgm(String rgm) { this.rgm = rgm; }

    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }
}