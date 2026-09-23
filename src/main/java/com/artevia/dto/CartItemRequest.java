package com.artevia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemRequest(

        @NotNull(message = "L'id del prodotto è obbligatorio")
        Long id,

        @NotNull(message = "La quantità è obbligatoria")
        @Positive(message = "La quantità deve essere maggiore di zero")
        @Max(value = 100, message = "Quantità massima 100 per prodotto")
        Integer quantity
) {}