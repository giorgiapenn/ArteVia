package com.artevia.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank(message = "Lo username è obbligatorio") String username,
    @NotBlank(message = "Il nome è obbligatorio") String name,
    @NotBlank(message = "Il cognome è obbligatorio") String lastname,

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Formato email non valido")
    String email,

    String address,

    @Min(value = 14, message = "Età minima 14 anni")
    @Max(value = 120, message = "Età massima 120 anni")
    Integer age,

    @NotBlank(message = "La password è obbligatoria")
    @Size(min = 8, max = 72, message = "La password deve avere tra 8 e 72 caratteri")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!]).*$",
        message = "La password deve contenere almeno una maiuscola, una minuscola, un numero e un carattere speciale"
    )
    String password
) {}