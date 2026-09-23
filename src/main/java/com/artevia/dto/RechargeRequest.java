package com.artevia.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RechargeRequest(
        @NotNull(message = "L'importo è obbligatorio")
        @DecimalMin(value = "0.01", message = "L'importo minimo è 0,01 €")
        @DecimalMax(value = "10000.00", message = "L'importo massimo per ricarica è 10.000 €")
        @Digits(integer = 5, fraction = 2, message = "Massimo due decimali")
        BigDecimal amount
) {}