package com.e_library.modules.user.repository;

import java.util.Optional;
import java.util.UUID; // Importação do UUID adicionada

import org.springframework.data.jpa.repository.JpaRepository;
import com.e_library.modules.user.entity.UserEntity;

// Alterado de <UserEntity, Long> para <UserEntity, UUID>
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
}