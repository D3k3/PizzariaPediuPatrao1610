package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.HistoricoStatus;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.service.CalculoPedido;
import com.umc.pediupatrao.service.FluxoStatusPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardSeedTest {
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private final Clock relogio = Clock.fixed(Instant.parse("2026-06-10T18:00:00Z"), FUSO);

    private PedidoRepository pedidoRepository;
    private ClienteRepository clienteRepository;
    private ProdutoRepository produtoRepository;
    private final List<Pedido> pedidosSalvos = new ArrayList<>();
    private final List<Produto> produtos = new ArrayList<>();
    private final List<Cliente> clientes = new ArrayList<>();

    @BeforeEach
    void preparar() {
        pedidoRepository = mock(PedidoRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        produtoRepository = mock(ProdutoRepository.class);
        AtomicInteger ids = new AtomicInteger();

        when(pedidoRepository.count()).thenAnswer(i -> (long) pedidosSalvos.size());
        when(pedidoRepository.saveAll(anyList())).thenAnswer(i -> {
            pedidosSalvos.addAll(i.<List<Pedido>>getArgument(0));
            return i.getArgument(0);
        });
        when(produtoRepository.count()).thenAnswer(i -> (long) produtos.size());
        when(produtoRepository.findAll()).thenAnswer(i -> new ArrayList<>(produtos));
        when(produtoRepository.saveAll(anyList())).thenAnswer(i -> {
            List<Produto> novos = i.getArgument(0);
            novos.forEach(p -> p.setId("p" + ids.incrementAndGet()));
            produtos.addAll(novos);
            return novos;
        });
        when(clienteRepository.count()).thenAnswer(i -> (long) clientes.size());
        when(clienteRepository.findAll()).thenAnswer(i -> new ArrayList<>(clientes));
        when(clienteRepository.saveAll(anyList())).thenAnswer(i -> {
            List<Cliente> novos = i.getArgument(0);
            novos.forEach(c -> c.setId("c" + ids.incrementAndGet()));
            clientes.addAll(novos);
            return novos;
        });
    }

    private DashboardSeed seed() {
        return new DashboardSeed(pedidoRepository, clienteRepository, produtoRepository, relogio);
    }

    @Test
    void soExisteNoPerfilLocal() {
        Profile profile = DashboardSeed.class.getAnnotation(Profile.class);
        assertEquals(List.of("local"), List.of(profile.value()));
    }

    @Test
    void criaProdutosClientesEPedidosComADistribuicaoPedida() {
        seed().run();

        assertEquals(10, produtos.size());
        assertEquals(8, clientes.size());
        assertEquals(120, pedidosSalvos.size());

        Map<StatusPedido, Long> porStatus = pedidosSalvos.stream()
                .collect(Collectors.groupingBy(Pedido::getStatus, Collectors.counting()));
        assertEquals(90L, porStatus.get(StatusPedido.FINALIZADO));
        assertEquals(10L, porStatus.get(StatusPedido.CANCELADO));
        assertTrue(porStatus.keySet().containsAll(List.of(StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO,
                StatusPedido.PRONTO, StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.RETIRADO)));
        assertTrue(pedidosSalvos.stream().anyMatch(p -> p.getPercentualDesconto() != null));
        assertTrue(pedidosSalvos.stream().filter(p -> p.getPercentualDesconto() != null)
                .allMatch(p -> p.getPercentualDesconto().compareTo(BigDecimal.valueOf(20)) <= 0));
    }

    @Test
    void valoresEHistoricoSaoCoerentes() {
        seed().run();
        LocalDateTime agora = LocalDateTime.now(relogio);

        for (Pedido p : pedidosSalvos) {
            assertEquals(CalculoPedido.valorBruto(p.getItens()), p.getValorBruto());
            assertEquals(CalculoPedido.valorTotal(p.getValorBruto(), p.getPercentualDesconto()), p.getValorTotal());
            for (ItemPedido i : p.getItens()) {
                assertEquals(CalculoPedido.subtotal(i.getPrecoUnitario(), i.getQuantidade()), i.getSubtotal());
            }

            List<HistoricoStatus> h = p.getHistoricoStatus();
            assertEquals(StatusPedido.RECEBIDO, h.get(0).getStatus());
            assertEquals(p.getDataEntrada(), h.get(0).getDataHora());
            assertEquals(p.getStatus(), h.get(h.size() - 1).getStatus());
            for (int i = 1; i < h.size(); i++) {
                StatusPedido anterior = h.get(i - 1).getStatus();
                StatusPedido atual = h.get(i).getStatus();
                if (atual != StatusPedido.CANCELADO) {
                    assertTrue(FluxoStatusPedido.podeTransitar(anterior, atual, p.getTipoAtendimento()),
                            anterior + " -> " + atual);
                } else {
                    assertTrue(anterior.isAntesDaSaida());
                }
                assertFalse(h.get(i).getDataHora().isBefore(h.get(i - 1).getDataHora()));
            }
            assertFalse(h.get(h.size() - 1).getDataHora().isAfter(agora));
            assertFalse(p.getDataEntrada().isBefore(agora.minusDays(45).toLocalDate().atStartOfDay()));

            boolean teveSaida = h.stream().anyMatch(x -> x.getStatus().isSaida());
            assertEquals(teveSaida, p.getDataSaida() != null);
            assertEquals(teveSaida, p.getResponsavelSaida() != null);
            if (p.getStatus() == StatusPedido.CANCELADO) {
                assertNull(p.getDataSaida());
                assertFalse(p.getJustificativaCancelamento().isBlank());
                assertEquals("gerente", p.getCanceladoPor());
                assertEquals(h.get(h.size() - 1).getDataHora(), p.getDataCancelamento());
            }
        }
    }

    @Test
    void horariosConcentradosEMaisMovimentoNoFimDeSemana() {
        seed().run();
        List<Pedido> vendas = pedidosSalvos.stream()
                .filter(p -> p.getStatus() == StatusPedido.FINALIZADO).toList();

        long emPico = vendas.stream().map(p -> p.getDataEntrada().getHour())
                .filter(h -> (h >= 11 && h < 14) || (h >= 18 && h < 22)).count();
        assertTrue(emPico >= vendas.size() * 0.85, "horários em pico: " + emPico);

        long noFim = vendas.stream().filter(p -> {
            DayOfWeek d = p.getDataEntrada().getDayOfWeek();
            return d == DayOfWeek.FRIDAY || d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
        }).count();
        assertTrue(noFim > vendas.size() / 2, "sex-dom: " + noFim);
    }

    @Test
    void naoDuplicaQuandoJaHaPedidos() {
        seed().run();
        int antes = pedidosSalvos.size();
        seed().run();
        assertEquals(antes, pedidosSalvos.size());
    }

    @Test
    void nadaFazSeColecaoDePedidosNaoEstiverVazia() {
        when(pedidoRepository.count()).thenReturn(1L);
        seed().run();
        verify(pedidoRepository, never()).saveAll(anyList());
        verify(produtoRepository, never()).saveAll(anyList());
        verify(clienteRepository, never()).saveAll(anyList());
    }

    @Test
    void reutilizaProdutosEClientesExistentes() {
        Produto p = new Produto();
        p.setId("x1");
        p.setNome("Existente");
        p.setPreco(30.0);
        p.setAtivo(true);
        produtos.add(p);
        Cliente c = new Cliente();
        c.setId("cx1");
        clientes.add(c);

        seed().run();

        assertEquals(1, produtos.size());
        assertEquals(1, clientes.size());
        assertEquals(120, pedidosSalvos.size());
        assertTrue(pedidosSalvos.stream().allMatch(x -> x.getClienteId().equals("cx1")));
    }
}
