package com.artevia.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank(message = "Lo username è obbligatorio")
    @Pattern(regexp = "^[A-Za-z0-9_.-]{3,30}$", message = "Lo username deve avere 3-30 caratteri tra lettere, numeri, punto, trattino e underscore")
    String username,

    @NotBlank(message = "Il nome è obbligatorio")
    @Size(max = 100, message = "Il nome non può superare 100 caratteri")
    String name,

    @NotBlank(message = "Il cognome è obbligatorio")
    @Size(max = 100, message = "Il cognome non può superare 100 caratteri")
    String lastname,

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Formato email non valido")
    String email,

    @Size(max = 255, message = "L'indirizzo non può superare 255 caratteri")
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