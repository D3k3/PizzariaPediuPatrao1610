package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.TipoAtendimento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PedidoRequest {
    @NotBlank(message = "Informe o cliente.")
    private String clienteId;

    @NotNull(message = "Informe o tipo de atendimento.")
    private TipoAtendimento tipoAtendimento;

    @NotEmpty(message = "O pedido precisa de pelo menos um item.")
    private List<@Valid ItemPedidoRequest> itens = new ArrayList<>();

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public TipoAtendimento getTipoAtendimento() { return tipoAtendimento; }
    public void setTipoAtendimento(TipoAtendimento tipoAtendimento) { this.tipoAtendimento = tipoAtendimento; }

    public List<ItemPedidoRequest> getItens() { return itens; }
    public void setItens(List<ItemPedidoRequest> itens) { this.itens = itens; }
}
