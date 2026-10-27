package com.e_library.modules.user.dto;

public class AuthResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String role;
    private final String name;

    public AuthResponse(String accessToken, String refreshToken, String role, String name) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.role = role;
        this.name = name;
    }

    public String getAccessToken() { return accessToken; }

    public String getRefreshToken() { return refreshToken; }

    // aluno, professor ou bibliotecario
    public String getRole() { return role; }

    public String getName() { return name; }
}
