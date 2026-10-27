package com.e_library.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.
        UsernamePasswordAuthenticationToken;

import org.springframework.security.core.context.
        SecurityContextHolder;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.security.web.authentication.
        WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;

import com.e_library.modules.user.service.CustomUserDetailsService;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;

    private final SupabaseAuthClient supabaseAuth;

    public JwtFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            SupabaseAuthClient supabaseAuth
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.supabaseAuth = supabaseAuth;
    }

    @Override
    protected void doFilterInternal(
            @NonNull     HttpServletRequest request,
            @NonNull    HttpServletResponse response,
            @NonNull    FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authHeader.substring(7);

        // 1) token emitido pelo próprio back; 2) token do login do Supabase
        String email = null;

        // Só token de ACESSO do próprio back vale (token de refresh não abre a API)
        if (jwtService.isValid(token) && jwtService.isAccessToken(token)) {
            email = jwtService.extractEmail(token);
        } else {
            email = supabaseAuth.emailFromAccessToken(token);
        }

        if (email == null) {
            filterChain.doFilter(request, response);
            return;
        }

        UserDetails userDetails;

        try {
            // carrega o papel (aluno/professor/bibliotecario) da tabela usuarios
            userDetails = userDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            filterChain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        authToken.setDetails(
                new WebAuthenticationDetailsSource()
                        .buildDetails(request)
        );

        SecurityContextHolder.getContext()
                .setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
