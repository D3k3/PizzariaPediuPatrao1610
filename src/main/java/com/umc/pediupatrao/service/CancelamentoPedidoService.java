package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.CancelamentoRequest;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.EntidadeAuditada;
import com.umc.pediupatrao.entity.OperacaoAuditoria;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CancelamentoPedidoService {

    private final PedidoRepository pedidoRepository;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    public CancelamentoPedidoService(PedidoRepository pedidoRepository, AuditoriaService auditoriaService,
            Clock relogio) {
        this.pedidoRepository = pedidoRepository;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(String pedidoId, String justificativa) {
        String motivo = justificativa == null ? "" : justificativa.trim();
        if (motivo.isEmpty()) {
            throw new IllegalArgumentException("A justificativa do cancelamento é obrigatória.");
        }
        if (motivo.length() > CancelamentoRequest.JUSTIFICATIVA_MAXIMA) {
            throw new IllegalArgumentException("A justificativa deve ter no máximo 500 caracteres.");
        }

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado."));
        if (pedido.isCancelado()) {
            throw new RegraNegocioException("Pedido cancelado não pode ser alterado.");
        }
        if (pedido.getStatus() == null || !pedido.getStatus().isAntesDaSaida()) {
            throw new RegraNegocioException("Só é possível cancelar antes da saída do pedido (status atual: "
                    + pedido.getStatus() + ").");
        }

        String responsavel = UsuarioLogado.exigirUsername();
        LocalDateTime agora = LocalDateTime.now(relogio);
        StatusPedido anterior = pedido.getStatus();

        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setJustificativaCancelamento(motivo);
        pedido.setCanceladoPor(responsavel);
        pedido.setDataCancelamento(agora);
        pedido.registrarHistorico(StatusPedido.CANCELADO, agora, responsavel);

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(OperacaoAuditoria.CANCELAMENTO, EntidadeAuditada.PEDIDO, salvo.getId(),
                List.of(new AlteracaoCampo("status", anterior.name(), StatusPedido.CANCELADO.name()),
                        new AlteracaoCampo("justificativaCancelamento", null, motivo)),
                motivo);
        return salvo;
    }
}
