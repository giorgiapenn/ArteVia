package com.artevia.dto;

import java.math.BigDecimal;

public record ProductDto(Long id, String name, String description, BigDecimal price, Integer stockQuantity, String category) {}