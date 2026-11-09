package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VendaDiaDto(LocalDate data, BigDecimal faturamento, long quantidade) {
}
