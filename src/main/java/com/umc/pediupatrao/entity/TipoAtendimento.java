package com.umc.pediupatrao.entity;

public enum TipoAtendimento {
    ENTREGA("Entrega"),
    RETIRADA("Retirada");

    private final String descricao;

    TipoAtendimento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
