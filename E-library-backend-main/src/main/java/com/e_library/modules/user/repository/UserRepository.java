package com.e_library.modules.user.repository;

import java.util.Optional;
import com.e_library.modules.user.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
}

