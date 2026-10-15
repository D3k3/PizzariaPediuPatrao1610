package com.umc.pediupatrao.entity;

import java.util.EnumSet;
import java.util.Set;

public enum StatusPedido {
    RECEBIDO("Recebido"),
    EM_PREPARACAO("Em preparação"),
    PRONTO("Pronto"),
    SAIU_PARA_ENTREGA("Saiu para entrega"),
    RETIRADO("Retirado"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    private static final Set<StatusPedido> ANTES_DA_SAIDA = EnumSet.of(RECEBIDO, EM_PREPARACAO, PRONTO);
    private static final Set<StatusPedido> SAIDA = EnumSet.of(SAIU_PARA_ENTREGA, RETIRADO);

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isAntesDaSaida() {
        return ANTES_DA_SAIDA.contains(this);
    }

    public boolean isSaida() {
        return SAIDA.contains(this);
    }
}
