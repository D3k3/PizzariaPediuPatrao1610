package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.ItemPedido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class CalculoPedido {
    public static final int CASAS = 2;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_UP;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private CalculoPedido() {
    }

    public static BigDecimal preco(Double valor) {
        return BigDecimal.valueOf(valor).setScale(CASAS, ARREDONDAMENTO);
    }

    public static BigDecimal subtotal(BigDecimal precoUnitario, int quantidade) {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade)).setScale(CASAS, ARREDONDAMENTO);
    }

    public static BigDecimal valorBruto(List<ItemPedido> itens) {
        return itens.stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(CASAS, ARREDONDAMENTO);
    }

    public static BigDecimal valorTotal(BigDecimal valorBruto, BigDecimal percentualDesconto) {
        if (percentualDesconto == null || percentualDesconto.signum() == 0) {
            return valorBruto.setScale(CASAS, ARREDONDAMENTO);
        }
        BigDecimal fator = BigDecimal.ONE.subtract(percentualDesconto.divide(CEM));
        return valorBruto.multiply(fator).setScale(CASAS, ARREDONDAMENTO);
    }
}
