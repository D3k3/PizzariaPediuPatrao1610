package com.umc.pediupatrao.dto.dashboard;

import com.umc.pediupatrao.entity.StatusPedido;

public record StatusQuantidadeDto(StatusPedido status, String descricao, long quantidade) {
}
