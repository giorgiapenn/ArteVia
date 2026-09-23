package com.artevia.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletDto(Long id, BigDecimal balance, LocalDateTime createdAt) {}