package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;

public record ResumoDashboardResponse(
        PeriodoDto periodo,
        BigDecimal faturamento,
        long quantidadeVendas,
        BigDecimal ticketMedio,
        long quantidadeCancelados,
        BigDecimal totalDescontos) {
}
