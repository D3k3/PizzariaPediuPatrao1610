package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.AuditoriaEventoResponse;
import com.umc.pediupatrao.dto.FiltroAuditoria;
import com.umc.pediupatrao.dto.PaginaResponse;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.AuditoriaEvento;
import com.umc.pediupatrao.entity.EntidadeAuditada;
import com.umc.pediupatrao.entity.OperacaoAuditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaService {
    public static final int TAMANHO_PAGINA_PADRAO = 20;
    public static final int TAMANHO_PAGINA_MAXIMO = 100;
    private static final String CAMPO_TELEFONE = "telefone";

    private final AuditoriaRepository auditoriaRepository;
    private final Clock relogio;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, Clock relogio) {
        this.auditoriaRepository = auditoriaRepository;
        this.relogio = relogio;
    }

    public AuditoriaEvento registrar(OperacaoAuditoria operacao, EntidadeAuditada entidade, String entidadeId,
            List<AlteracaoCampo> alteracoes, String justificativa) {
        List<AlteracaoCampo> seguras = alteracoes.stream()
                .filter(a -> !ComparadorCampos.isSensivel(a.campo()))
                .toList();
        AuditoriaEvento evento = new AuditoriaEvento(null,
                UsuarioLogado.exigirUsername(),
                UsuarioLogado.perfil().orElse(null),
                LocalDateTime.now(relogio),
                operacao, entidade, entidadeId, seguras, justificativa);
        return auditoriaRepository.save(evento);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public PaginaResponse<AuditoriaEventoResponse> consultar(FiltroAuditoria filtro, int pagina, int tamanho) {
        int tamanhoValido = tamanho <= 0 ? TAMANHO_PAGINA_PADRAO : Math.min(tamanho, TAMANHO_PAGINA_MAXIMO);
        PageRequest paginacao = PageRequest.of(Math.max(pagina, 0), tamanhoValido);
        FiltroAuditoria criterios = filtro == null ? new FiltroAuditoria() : filtro;
        if (criterios.getDe() != null && criterios.getAte() != null && criterios.getDe().isAfter(criterios.getAte())) {
            throw new IllegalArgumentException("A data inicial deve ser anterior ou igual à data final.");
        }
        return PaginaResponse.de(auditoriaRepository.consultar(criterios, paginacao), this::paraResposta);
    }

    AuditoriaEventoResponse paraResposta(AuditoriaEvento evento) {
        List<AlteracaoCampo> alteracoes = evento.getAlteracoes().stream()
                .map(a -> CAMPO_TELEFONE.equals(a.campo())
                        ? new AlteracaoCampo(a.campo(), MascaraDados.telefone(a.valorAnterior()),
                                MascaraDados.telefone(a.valorNovo()))
                        : a)
                .toList();
        return new AuditoriaEventoResponse(evento.getId(), evento.getUsuario(), evento.getPerfil(),
                evento.getDataHora(), evento.getOperacao(), evento.getEntidade(), evento.getEntidadeId(),
                alteracoes, evento.getJustificativa());
    }
}
