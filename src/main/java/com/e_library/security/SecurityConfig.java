package com.e_library.security;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Sem login válido = 401 (antes aparecia como 403)
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                // permite a rota de erro do Spring, senão qualquer erro interno vira 403 sem explicação
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                .requestMatchers("/auth/**").permitAll()
                
                // EXIGE PAPEL DE BIBLIOTECÁRIO PARA CRIAR, EDITAR OU DELETAR LIVROS E CATEGORIAS
                .requestMatchers(HttpMethod.POST, "/api/livros/**").hasAnyAuthority("BIBLIOTECARIO", "bibliotecario", "ROLE_BIBLIOTECARIO", "ROLE_bibliotecario")
                .requestMatchers(HttpMethod.PUT, "/api/livros/**").hasAnyAuthority("BIBLIOTECARIO", "bibliotecario", "ROLE_BIBLIOTECARIO", "ROLE_bibliotecario")
                .requestMatchers(HttpMethod.DELETE, "/api/livros/**").hasAnyAuthority("BIBLIOTECARIO", "bibliotecario", "ROLE_BIBLIOTECARIO", "ROLE_bibliotecario")
                
                // PAINEL DO BIBLIOTECÁRIO: listar solicitações/emprestados, entregar e registrar devolução
                .requestMatchers(HttpMethod.GET, "/api/emprestimos/solicitados", "/api/emprestimos/retirados", "/api/emprestimos/renegociacoes", "/api/emprestimos/extraviados", "/api/multas").hasAuthority("ROLE_BIBLIOTECARIO")
                .requestMatchers(HttpMethod.PUT, "/api/emprestimos/*/retirar", "/api/emprestimos/*/devolver", "/api/emprestimos/*/extravio", "/api/emprestimos/*/reposicao", "/api/emprestimos/*/baixa", "/api/emprestimos/*/renegociacao/*").hasAuthority("ROLE_BIBLIOTECARIO")

                // SUPORTE DE ACESSO: qualquer pessoa pode abrir um pedido (a página pública não exige login)
                .requestMatchers(HttpMethod.POST, "/api/suporte/acesso").permitAll()

                // Painel de suporte, anexos e relatórios: só bibliotecário
                .requestMatchers("/api/suporte/**", "/api/relatorios/**").hasAuthority("ROLE_BIBLIOTECARIO")

                // ACERVO COMPLETO (ativos e inativos, com filtros): só bibliotecário
                .requestMatchers(HttpMethod.GET, "/api/livros/acervo").hasAuthority("ROLE_BIBLIOTECARIO")

                // EXEMPLARES (cadastro, consulta e códigos): só bibliotecário
                .requestMatchers("/api/exemplares/**").hasAuthority("ROLE_BIBLIOTECARIO")

                // PERMITE QUE QUALQUER UTILIZADOR LOGADO (ALUNOS) POSSA APENAS LER/VER OS LIVROS E CATEGORIAS
                .requestMatchers(HttpMethod.GET, "/api/livros/**").authenticated()
                
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); 
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
