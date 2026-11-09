package com.umc.pediupatrao.dto.dashboard;

import com.umc.pediupatrao.entity.TipoAtendimento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UltimoPedidoDto(
        String id,
        String idCurto,
        LocalDateTime dataFinalizacao,
        TipoAtendimento tipoAtendimento,
        int quantidadeItens,
        BigDecimal valorTotal,
        BigDecimal diferencaTicketMedio,
        BigDecimal diferencaTicketMedioPercentual) {
}
