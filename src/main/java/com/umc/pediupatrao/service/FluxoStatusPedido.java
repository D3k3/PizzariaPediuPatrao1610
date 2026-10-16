package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;

import java.util.List;

/**
 * Mapa das transições de status permitidas pelo fluxo normal do pedido.
 * O cancelamento não passa por aqui: tem regra e rota próprias (exige justificativa).
 */
public final class FluxoStatusPedido {

    private FluxoStatusPedido() {
    }

    /** Próximos status possíveis; a saída depende do tipo de atendimento. */
    public static List<StatusPedido> proximos(StatusPedido atual, TipoAtendimento tipo) {
        if (atual == null) {
            return List.of();
        }
        return switch (atual) {
            case RECEBIDO -> List.of(StatusPedido.EM_PREPARACAO);
            case EM_PREPARACAO -> List.of(StatusPedido.PRONTO);
            case PRONTO -> tipo == null ? List.of()
                    : List.of(tipo == TipoAtendimento.ENTREGA ? StatusPedido.SAIU_PARA_ENTREGA : StatusPedido.RETIRADO);
            case SAIU_PARA_ENTREGA, RETIRADO -> List.of(StatusPedido.FINALIZADO);
            case FINALIZADO, CANCELADO -> List.of();
        };
    }

    public static boolean podeTransitar(StatusPedido origem, StatusPedido destino, TipoAtendimento tipo) {
        return proximos(origem, tipo).contains(destino);
    }
}
