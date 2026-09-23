package com.artevia.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchasedProductDto(
    String productName, Integer quantity, BigDecimal priceAtPurchase,
    BigDecimal total, LocalDateTime purchasedAt) {}