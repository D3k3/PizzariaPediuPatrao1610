package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;

public record ProdutoRankingDto(
        String produtoId,
        String nome,
        long quantidadeVendida,
        BigDecimal receita,
        BigDecimal percentualDoFaturamento) {
}
