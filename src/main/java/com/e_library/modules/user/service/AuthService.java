package com.e_library.modules.user.service;


import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.
        BCryptPasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.e_library.modules.user.UserEntity;
import com.e_library.modules.user.UserRepository;
import com.e_library.modules.user.dto.AuthResponse;
import com.e_library.modules.user.dto.LoginRequest;
import com.e_library.modules.user.dto.RefreshRequest;
import com.e_library.modules.user.dto.RegisterRequest;
import com.e_library.modules.user.dto.VerifyRequest;
import com.e_library.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;

    private final JwtService jwtService;

    private final BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder();

    public AuthService(
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
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

        UserEntity user =
                userRepository.findByEmail(
                        request.getEmail()
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Email ou senha inválidos"
                        )
                );

        boolean passwordCorrect =
                encoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if (!passwordCorrect) {
            throw new RuntimeException(
                    "Invalid password"
            );
        }

        String accessToken =
                jwtService.generateAccessToken(
                        user.getEmail()
                );

        String refreshToken =
                jwtService.generateRefreshToken(
                        user.getEmail()
                );

        return new AuthResponse(
                accessToken,
                refreshToken
        );
    }

    public AuthResponse refresh(
            RefreshRequest request
    ) {

        String email =
                jwtService.extractEmail(
                        request.getRefreshToken()
                );

        String newAccessToken =
                jwtService.generateAccessToken(email);

        String newRefreshToken =
                jwtService.generateRefreshToken(email);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken
        );
    }

    public boolean verify(
            VerifyRequest request
    ) {
        return jwtService.isValid(request.getToken());
    }
}