package com.artevia.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Username o email obbligatori") String usernameOrEmail,
    @NotBlank(message = "La password è obbligatoria") String password
) {}