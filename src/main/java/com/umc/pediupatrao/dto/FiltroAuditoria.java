package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.EntidadeAuditada;
import com.umc.pediupatrao.entity.OperacaoAuditoria;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class FiltroAuditoria {
    private EntidadeAuditada entidade;
    private String entidadeId;
    private OperacaoAuditoria operacao;
    private String usuario;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate de;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ate;

    public EntidadeAuditada getEntidade() { return entidade; }
    public void setEntidade(EntidadeAuditada entidade) { this.entidade = entidade; }

    public String getEntidadeId() { return entidadeId; }
    public void setEntidadeId(String entidadeId) { this.entidadeId = entidadeId; }

    public OperacaoAuditoria getOperacao() { return operacao; }
    public void setOperacao(OperacaoAuditoria operacao) { this.operacao = operacao; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public LocalDate getDe() { return de; }
    public void setDe(LocalDate de) { this.de = de; }

    public LocalDate getAte() { return ate; }
    public void setAte(LocalDate ate) { this.ate = ate; }
}
