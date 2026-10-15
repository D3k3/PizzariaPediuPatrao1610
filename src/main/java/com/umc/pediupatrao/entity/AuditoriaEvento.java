package com.umc.pediupatrao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "auditoria")
public class AuditoriaEvento {
    @Id
    private String id;
    private final String usuario;
    private final Perfil perfil;
    private final LocalDateTime dataHora;
    private final OperacaoAuditoria operacao;
    private final EntidadeAuditada entidade;
    private final String entidadeId;
    private final List<AlteracaoCampo> alteracoes;
    private final String justificativa;

    @PersistenceCreator
    public AuditoriaEvento(String id, String usuario, Perfil perfil, LocalDateTime dataHora,
            OperacaoAuditoria operacao, EntidadeAuditada entidade, String entidadeId,
            List<AlteracaoCampo> alteracoes, String justificativa) {
        this.id = id;
        this.usuario = usuario;
        this.perfil = perfil;
        this.dataHora = dataHora;
        this.operacao = operacao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.alteracoes = alteracoes == null ? List.of() : List.copyOf(alteracoes);
        this.justificativa = justificativa;
    }

    public String getId() { return id; }
    public String getUsuario() { return usuario; }
    public Perfil getPerfil() { return perfil; }
    public LocalDateTime getDataHora() { return dataHora; }
    public OperacaoAuditoria getOperacao() { return operacao; }
    public EntidadeAuditada getEntidade() { return entidade; }
    public String getEntidadeId() { return entidadeId; }
    public List<AlteracaoCampo> getAlteracoes() { return alteracoes; }
    public String getJustificativa() { return justificativa; }
}
