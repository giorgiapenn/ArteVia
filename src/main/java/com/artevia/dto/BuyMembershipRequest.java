package com.artevia.dto;

import jakarta.validation.constraints.NotNull;

public record BuyMembershipRequest(
        @NotNull(message = "L'id del piano è obbligatorio")
        Long planId
) {}