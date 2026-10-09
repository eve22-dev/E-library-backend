package com.e_library.modules.user.controller;

import com.e_library.modules.user.dto.AuthResponse;
import com.e_library.modules.user.dto.LoginRequest;
import com.e_library.modules.user.dto.RefreshRequest;
import com.e_library.modules.user.dto.RegisterRequest;
import com.e_library.modules.user.dto.VerifyRequest;
import com.e_library.modules.user.service.AuthService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService
    ) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public String register(
            @RequestBody RegisterRequest request
    ) {

        authService.register(request);

        return "User created";
    }

    @PostMapping("/login")
    public AuthResponse login(
            @RequestBody LoginRequest request
    ) {

        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @RequestBody RefreshRequest request
    ) {

        return authService.refresh(request);
    }

    @PostMapping("/verify")
    public boolean verify(
            @RequestBody VerifyRequest request
    ) {

        return authService.verify(request);
    }

    @GetMapping("/me")
    public String me() {
        return "Authenticated";
    }
}
