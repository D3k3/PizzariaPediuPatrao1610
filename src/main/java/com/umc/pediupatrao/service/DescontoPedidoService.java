package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.EntidadeAuditada;
import com.umc.pediupatrao.entity.OperacaoAuditoria;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Desconto percentual sobre o pedido inteiro: exclusivo do GERENTE, 0 < p ≤ 20,
 * só antes da saída. Um novo desconto substitui o anterior (não acumula).
 */
@Service
public class DescontoPedidoService {

    public static final BigDecimal PERCENTUAL_MAXIMO = new BigDecimal("20");
    private static final int CASAS_PERCENTUAL = 2;

    private final PedidoRepository pedidoRepository;
    private final AuditoriaService auditoriaService;

    public DescontoPedidoService(PedidoRepository pedidoRepository, AuditoriaService auditoriaService) {
        this.pedidoRepository = pedidoRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicar(String pedidoId, BigDecimal percentual) {
        validarPercentual(percentual);

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado."));
        if (pedido.isCancelado()) {
            throw new RegraNegocioException("Pedido cancelado não pode ser alterado.");
        }
        if (pedido.getStatus() == null || !pedido.getStatus().isAntesDaSaida()) {
            throw new RegraNegocioException("Desconto só pode ser aplicado antes da saída do pedido (status atual: "
                    + pedido.getStatus() + ").");
        }
        if (pedido.getValorBruto() == null) {
            throw new RegraNegocioException("Pedido antigo, sem itens e valores, não admite desconto.");
        }

        BigDecimal percentualAnterior = pedido.getPercentualDesconto();
        BigDecimal totalAnterior = pedido.getValorTotal();
        BigDecimal percentualNovo = percentual.setScale(CASAS_PERCENTUAL);

        pedido.setPercentualDesconto(percentualNovo);
        pedido.setValorTotal(CalculoPedido.valorTotal(pedido.getValorBruto(), percentualNovo));

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(OperacaoAuditoria.DESCONTO, EntidadeAuditada.PEDIDO, salvo.getId(),
                List.of(new AlteracaoCampo("percentualDesconto",
                                ComparadorCampos.formatar(percentualAnterior), percentualNovo.toPlainString()),
                        new AlteracaoCampo("valorTotal",
                                ComparadorCampos.formatar(totalAnterior), salvo.getValorTotal().toPlainString())),
                null);
        return salvo;
    }

    private static void validarPercentual(BigDecimal percentual) {
        if (percentual == null) {
            throw new IllegalArgumentException("Informe o percentual de desconto.");
        }
        if (percentual.signum() <= 0) {
            throw new IllegalArgumentException("O desconto deve ser maior que 0%.");
        }
        if (percentual.compareTo(PERCENTUAL_MAXIMO) > 0) {
            throw new IllegalArgumentException("O desconto não pode passar de 20%.");
        }
        if (percentual.stripTrailingZeros().scale() > CASAS_PERCENTUAL) {
            throw new IllegalArgumentException("Use no máximo 2 casas decimais no percentual.");
        }
    }
}
