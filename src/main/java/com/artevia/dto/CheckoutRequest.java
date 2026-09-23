package com.artevia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CheckoutRequest(

        @NotEmpty(message = "Il carrello non può essere vuoto")
        @Size(max = 50, message = "Massimo 50 righe per carrello")
        List<@Valid CartItemRequest> items
) {}