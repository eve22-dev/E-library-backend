package com.e_library.modules.user.service;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import com.e_library.modules.user.entity.UserEntity;
import com.e_library.modules.user.repository.UserRepository;

@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        // papel no banco: aluno / professor / bibliotecario
        // vira ROLE_ALUNO / ROLE_PROFESSOR / ROLE_BIBLIOTECARIO no Spring Security
        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getEmail())
                .password(user.getPassword() == null ? "" : user.getPassword())
                .roles(user.getRole().toUpperCase())
                .build();
    }
}
