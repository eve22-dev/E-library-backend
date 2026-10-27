package com.e_library.modules.user.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.e_library.modules.user.dto.AuthResponse;
import com.e_library.modules.user.dto.LoginRequest;
import com.e_library.modules.user.dto.RefreshRequest;
import com.e_library.modules.user.dto.RegisterRequest;
import com.e_library.modules.user.dto.VerifyRequest;
import com.e_library.modules.user.entity.UserEntity;
import com.e_library.modules.user.repository.UserRepository;
import com.e_library.security.JwtService;
import com.e_library.security.SupabaseAuthClient;

@Service
public class AuthService {

    private final UserRepository userRepository;

    private final JwtService jwtService;

    private final SupabaseAuthClient supabaseAuth;

    private final BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder();

    public AuthService(
            UserRepository userRepository,
            JwtService jwtService,
            SupabaseAuthClient supabaseAuth
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.supabaseAuth = supabaseAuth;
    }

    public void register(RegisterRequest request) {

        boolean exists =
                userRepository.findByEmail(
                        request.getEmail()
                ).isPresent();

        if (exists) {
            throw new RuntimeException(
                    "Email already exists"
            );
        }

        UserEntity user = new UserEntity();

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        user.setPassword(
                encoder.encode(request.getPassword())
        );

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String senha = request.getPassword() == null ? "" : request.getPassword();

        if (email.isEmpty() || senha.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe e-mail e senha.");
        }

        // Tenta em minúsculas (como o Supabase guarda) e depois como foi digitado
        UserEntity user = userRepository.findByEmail(email.toLowerCase())
                .or(() -> userRepository.findByEmail(email))
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos"));

        // A senha é conferida no Supabase Auth, onde a conta foi criada
        if (!supabaseAuth.passwordIsValid(user.getEmail(), senha)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário inativo");
        }

        return buildResponse(user);
    }

    public AuthResponse refresh(
            RefreshRequest request
    ) {

        String refreshToken = request.getRefreshToken();

        if (refreshToken == null
                || !jwtService.isValid(refreshToken)
                || !jwtService.isRefreshToken(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada");
        }

        String email = jwtService.extractEmail(refreshToken);

        // Busca o usuário de novo: se o papel mudou ou ele foi desativado, vale o banco
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário inativo");
        }

        return buildResponse(user);
    }

    public boolean verify(
            VerifyRequest request
    ) {
        return jwtService.isValid(request.getToken());
    }

    private AuthResponse buildResponse(UserEntity user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user.getEmail(), user.getRole()),
                jwtService.generateRefreshToken(user.getEmail()),
                user.getRole(),
                user.getName()
        );
    }
}
