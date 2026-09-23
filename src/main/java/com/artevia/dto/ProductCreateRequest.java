package com.artevia.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductCreateRequest(

        @NotBlank(message = "Il nome del prodotto è obbligatorio")
        @Size(max = 150, message = "Il nome non può superare 150 caratteri")
        String name,

        @Size(max = 255, message = "La descrizione non può superare 255 caratteri")
        String description,

        @NotNull(message = "Il prezzo è obbligatorio")
        @DecimalMin(value = "0.01", message = "Il prezzo deve essere maggiore di zero")
        @Digits(integer = 8, fraction = 2, message = "Formato prezzo non valido")
        BigDecimal price,

        @NotNull(message = "La quantità in stock è obbligatoria")
        @PositiveOrZero(message = "Lo stock non può essere negativo")
        Integer stockQuantity,

        @NotBlank(message = "La categoria è obbligatoria")
        @Size(max = 50)
        String category
) {}