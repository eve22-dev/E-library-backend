package com.e_library.modules.user.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import com.e_library.modules.user.entity.UserEntity;
import com.e_library.modules.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String papel = user.getRole() == null ? "aluno" : user.getRole().toLowerCase();

        List<GrantedAuthority> permissoes = new ArrayList<>();
        permissoes.add(new SimpleGrantedAuthority("ROLE_" + papel.toUpperCase()));

        // O TI tem todos os poderes do bibliotecário, mais a criação de usuários
        if ("ti".equals(papel)) {
            permissoes.add(new SimpleGrantedAuthority("ROLE_BIBLIOTECARIO"));
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword() == null ? "" : user.getPassword())
                .authorities(permissoes)
                .build();
    }
}
