package com.umc.pediupatrao.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ItemPedidoRequest {
    public static final int QUANTIDADE_MAXIMA = 100;

    @NotBlank(message = "Informe o produto de cada item.")
    private String produtoId;

    @NotNull(message = "Informe a quantidade de cada item.")
    @Positive(message = "A quantidade deve ser maior que zero.")
    @Max(value = QUANTIDADE_MAXIMA, message = "Quantidade acima do permitido por item.")
    private Integer quantidade;

    public ItemPedidoRequest() {
    }

    public ItemPedidoRequest(String produtoId, Integer quantidade) {
        this.produtoId = produtoId;
        this.quantidade = quantidade;
    }

    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
}
