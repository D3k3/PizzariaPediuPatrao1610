package com.umc.pediupatrao.entity;

public enum OperacaoAuditoria {
    CRIACAO_PEDIDO("Criação de pedido"),
    ALTERACAO_PEDIDO("Alteração de pedido"),
    MUDANCA_STATUS("Mudança de status"),
    DESCONTO("Desconto"),
    CANCELAMENTO("Cancelamento"),
    CRIACAO_CLIENTE("Criação de cliente"),
    ALTERACAO_CLIENTE("Alteração de cliente"),
    EXCLUSAO_CLIENTE("Exclusão de cliente");

    private final String descricao;

    OperacaoAuditoria(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
