package com.umc.pediupatrao.exception;

import com.umc.pediupatrao.entity.StatusPedido;

public class TransicaoStatusInvalidaException extends RegraNegocioException {

    public TransicaoStatusInvalidaException(StatusPedido origem, StatusPedido destino, String motivo) {
        super("Transição inválida de " + origem + " para " + destino + ": " + motivo);
    }
}
