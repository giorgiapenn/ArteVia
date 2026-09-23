package com.artevia.dto;

import java.math.BigDecimal;

public record ClubPlanDto(Long id, String name, BigDecimal price, Integer durationDays, Integer discountPercentage) {}