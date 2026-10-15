package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.EntidadeAuditada;
import com.umc.pediupatrao.entity.OperacaoAuditoria;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.exception.TransicaoStatusInvalidaException;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/** Avanço do pedido pelo fluxo RECEBIDO → ... → FINALIZADO. */
@Service
public class StatusPedidoService {

    private final PedidoRepository pedidoRepository;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    public StatusPedidoService(PedidoRepository pedidoRepository, AuditoriaService auditoriaService, Clock relogio) {
        this.pedidoRepository = pedidoRepository;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Pedido avancar(String pedidoId, StatusPedido destino) {
        if (destino == null) {
            throw new IllegalArgumentException("Informe o novo status.");
        }
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado."));
        if (pedido.isCancelado()) {
            throw new RegraNegocioException("Pedido cancelado não pode ser alterado.");
        }

        StatusPedido origem = pedido.getStatus();
        if (!FluxoStatusPedido.podeTransitar(origem, destino, pedido.getTipoAtendimento())) {
            throw new TransicaoStatusInvalidaException(origem, destino, motivo(pedido, destino));
        }

        String responsavel = UsuarioLogado.exigirUsername();
        LocalDateTime agora = LocalDateTime.now(relogio);
        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        alteracoes.add(new AlteracaoCampo("status", origem.name(), destino.name()));

        pedido.setStatus(destino);
        if (destino.isSaida()) {
            pedido.setDataSaida(agora);
            pedido.setResponsavelSaida(responsavel);
            alteracoes.add(new AlteracaoCampo("dataSaida", null, agora.toString()));
            alteracoes.add(new AlteracaoCampo("responsavelSaida", null, responsavel));
        }
        pedido.registrarHistorico(destino, agora, responsavel);

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(OperacaoAuditoria.MUDANCA_STATUS, EntidadeAuditada.PEDIDO, salvo.getId(),
                alteracoes, null);
        return salvo;
    }

    private static String motivo(Pedido pedido, StatusPedido destino) {
        if (destino == StatusPedido.CANCELADO) {
            return "use a operação de cancelamento, que exige justificativa.";
        }
        if (pedido.getStatus() == StatusPedido.PRONTO && pedido.getTipoAtendimento() == null) {
            return "pedido antigo sem tipo de atendimento não pode registrar saída.";
        }
        List<StatusPedido> permitidos = FluxoStatusPedido.proximos(pedido.getStatus(), pedido.getTipoAtendimento());
        return permitidos.isEmpty() ? "o pedido já está em um status final."
                : "a partir de " + pedido.getStatus() + " só é permitido " + permitidos + ".";
    }
}
