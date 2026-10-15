package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.EntidadeAuditada;
import com.umc.pediupatrao.entity.OperacaoAuditoria;
import com.umc.pediupatrao.entity.Perfil;

import java.time.LocalDateTime;
import java.util.List;

public record AuditoriaEventoResponse(
        String id,
        String usuario,
        Perfil perfil,
        LocalDateTime dataHora,
        OperacaoAuditoria operacao,
        EntidadeAuditada entidade,
        String entidadeId,
        List<AlteracaoCampo> alteracoes,
        String justificativa) {
}
