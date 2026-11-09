package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.dashboard.Ordem;
import com.umc.pediupatrao.dto.dashboard.OrdenarPor;
import com.umc.pediupatrao.dto.dashboard.PicoDiaSemanaResponse;
import com.umc.pediupatrao.dto.dashboard.PicoHorarioResponse;
import com.umc.pediupatrao.dto.dashboard.ProdutoRankingDto;
import com.umc.pediupatrao.dto.dashboard.ResumoDashboardResponse;
import com.umc.pediupatrao.dto.dashboard.StatusQuantidadeDto;
import com.umc.pediupatrao.dto.dashboard.UltimoPedidoDto;
import com.umc.pediupatrao.dto.dashboard.VendaDiaDto;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    // Domingo, 2026-11-08, 12:00 em São Paulo
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 8);

    @Mock private PedidoRepository repo;
    private DashboardService service;
    private final List<Pedido> base = new ArrayList<>();
    private final List<Pedido> finalizadosRecentes = new ArrayList<>();
    private int seq = 0;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-08T15:00:00Z"), FUSO);
        service = new DashboardService(repo, clock);
        lenient().when(repo.findByDataEntradaBetween(any(), any())).thenAnswer(i -> new ArrayList<>(base));
        lenient().when(repo.findTop100ByStatusOrderByDataEntradaDesc(StatusPedido.FINALIZADO))
                .thenAnswer(i -> new ArrayList<>(finalizadosRecentes));
    }

    // ---------- helpers ----------

    private static ItemPedido item(String id, String nome, int qtd, String subtotal) {
        BigDecimal sub = new BigDecimal(subtotal);
        return new ItemPedido(id, nome, qtd, sub.divide(BigDecimal.valueOf(qtd), 2, java.math.RoundingMode.HALF_UP), sub);
    }

    private Pedido pedido(StatusPedido status, LocalDateTime entrada, TipoAtendimento tipo,
            String bruto, String total, ItemPedido... itens) {
        Pedido p = new Pedido();
        p.setId(String.format("%024x", ++seq));
        p.setStatus(status);
        p.setDataEntrada(entrada);
        p.setTipoAtendimento(tipo);
        p.setValorBruto(new BigDecimal(bruto));
        p.setValorTotal(new BigDecimal(total));
        p.setItens(new ArrayList<>(List.of(itens)));
        base.add(p);
        return p;
    }

    private Pedido venda(LocalDateTime entrada, String valor, ItemPedido... itens) {
        return pedido(StatusPedido.FINALIZADO, entrada, TipoAtendimento.ENTREGA, valor, valor, itens);
    }

    private static LocalDateTime dia(int d, int h, int m) {
        return LocalDateTime.of(2026, 11, d, h, m);
    }

    // ---------- resumo ----------

    @Test
    void resumo_soFinalizadoContaComoVenda() {
        venda(dia(5, 12, 0), "100.00", item("a", "A", 1, "100.00"));
        venda(dia(6, 12, 0), "50.00", item("a", "A", 1, "50.00"));
        pedido(StatusPedido.CANCELADO, dia(6, 13, 0), TipoAtendimento.ENTREGA, "80.00", "80.00", item("a", "A", 1, "80.00"));
        pedido(StatusPedido.RECEBIDO, dia(7, 13, 0), TipoAtendimento.ENTREGA, "30.00", "30.00", item("a", "A", 1, "30.00"));

        ResumoDashboardResponse r = service.resumo(null, null, null);

        assertThat(r.faturamento()).isEqualByComparingTo("150.00");
        assertThat(r.quantidadeVendas()).isEqualTo(2);
        assertThat(r.ticketMedio()).isEqualByComparingTo("75.00");
        assertThat(r.quantidadeCancelados()).isEqualTo(1);
        assertThat(r.periodo().inicio()).isEqualTo(HOJE.minusDays(29));
        assertThat(r.periodo().fim()).isEqualTo(HOJE);
    }

    @Test
    void resumo_ticketMedioZeroSemVendas() {
        pedido(StatusPedido.CANCELADO, dia(6, 13, 0), TipoAtendimento.ENTREGA, "80.00", "80.00", item("a", "A", 1, "80.00"));

        ResumoDashboardResponse r = service.resumo(null, null, null);

        assertThat(r.quantidadeVendas()).isZero();
        assertThat(r.ticketMedio()).isEqualByComparingTo("0");
        assertThat(r.faturamento()).isEqualByComparingTo("0");
        assertThat(r.ticketMedio().scale()).isEqualTo(2);
    }

    @Test
    void resumo_totalDeDescontosSomaBrutoMenosTotalDosFinalizados() {
        pedido(StatusPedido.FINALIZADO, dia(5, 12, 0), TipoAtendimento.ENTREGA, "100.00", "90.00", item("a", "A", 1, "100.00"));
        pedido(StatusPedido.FINALIZADO, dia(6, 12, 0), TipoAtendimento.ENTREGA, "200.00", "170.00", item("a", "A", 2, "200.00"));
        pedido(StatusPedido.CANCELADO, dia(6, 12, 0), TipoAtendimento.ENTREGA, "100.00", "50.00", item("a", "A", 1, "100.00"));

        assertThat(service.resumo(null, null, null).totalDescontos()).isEqualByComparingTo("40.00");
    }

    @Test
    void resumo_pedidoSemItensOuSemValorTotalEhIgnorado() {
        venda(dia(5, 12, 0), "100.00", item("a", "A", 1, "100.00"));
        Pedido semItens = pedido(StatusPedido.FINALIZADO, dia(5, 13, 0), TipoAtendimento.ENTREGA, "500.00", "500.00",
                item("a", "A", 1, "500.00"));
        semItens.setItens(new ArrayList<>());
        Pedido semTotal = pedido(StatusPedido.FINALIZADO, dia(5, 14, 0), TipoAtendimento.ENTREGA, "500.00", "500.00",
                item("a", "A", 1, "500.00"));
        semTotal.setValorTotal(null);

        ResumoDashboardResponse r = service.resumo(null, null, null);

        assertThat(r.quantidadeVendas()).isEqualTo(1);
        assertThat(r.faturamento()).isEqualByComparingTo("100.00");
    }

    @Test
    void filtroTipoAtendimentoAplicaEmTodosOsCalculos() {
        venda(dia(5, 12, 0), "100.00", item("a", "A", 1, "100.00")); // ENTREGA
        pedido(StatusPedido.FINALIZADO, dia(5, 13, 0), TipoAtendimento.RETIRADA, "60.00", "60.00", item("a", "A", 1, "60.00"));
        pedido(StatusPedido.CANCELADO, dia(5, 14, 0), TipoAtendimento.RETIRADA, "10.00", "10.00", item("a", "A", 1, "10.00"));

        ResumoDashboardResponse retirada = service.resumo(null, null, TipoAtendimento.RETIRADA);
        assertThat(retirada.faturamento()).isEqualByComparingTo("60.00");
        assertThat(retirada.quantidadeCancelados()).isEqualTo(1);

        assertThat(service.resumo(null, null, TipoAtendimento.ENTREGA).faturamento()).isEqualByComparingTo("100.00");
        assertThat(service.resumo(null, null, null).faturamento()).isEqualByComparingTo("160.00");
        assertThat(service.produtos(null, null, TipoAtendimento.RETIRADA, null, null, null)).hasSize(1);
    }

    @Test
    void periodoUsaInicioDoDiaAteFimDoDia() {
        service.resumo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), null);

        ArgumentCaptor<LocalDateTime> de = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> ate = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repo).findByDataEntradaBetween(de.capture(), ate.capture());
        assertThat(de.getValue()).isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
        assertThat(ate.getValue()).isEqualTo(LocalDateTime.of(2026, 11, 3, 23, 59, 59, 999_999_999));
    }

    // ---------- validação de período ----------

    @Test
    void periodo_inicioDepoisDoFim() {
        assertThatThrownBy(() -> service.resumo(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 4), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A data inicial não pode ser posterior à data final.");
    }

    @Test
    void periodo_maiorQue366Dias() {
        LocalDate fim = HOJE;
        assertThatThrownBy(() -> service.resumo(fim.minusDays(366), fim, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O período não pode exceder 366 dias.");
    }

    @Test
    void periodo_exatamente366DiasEhValido() {
        service.resumo(HOJE.minusDays(365), HOJE, null);
    }

    @Test
    void periodo_somenteInicioComMaisDe366DiasAteHoje() {
        assertThatThrownBy(() -> service.resumo(HOJE.minusDays(400), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O período não pode exceder 366 dias.");
    }

    @Test
    void periodo_somenteFimUsa30DiasParaTras() {
        LocalDate fim = LocalDate.of(2026, 10, 31);
        ResumoDashboardResponse r = service.resumo(null, fim, null);
        assertThat(r.periodo().inicio()).isEqualTo(fim.minusDays(29));
        assertThat(r.periodo().fim()).isEqualTo(fim);
    }

    @Test
    void periodo_fimNoFuturo() {
        assertThatThrownBy(() -> service.resumo(null, HOJE.plusDays(1), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A data final não pode estar no futuro.");
    }

    @Test
    void produtos_limiteForaDoIntervalo() {
        assertThatThrownBy(() -> service.produtos(null, null, null, null, null, 21))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.produtos(null, null, null, null, null, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- séries ----------

    @Test
    void vendasPorDia_umaEntradaPorDiaComZeros() {
        venda(dia(2, 12, 0), "100.00", item("a", "A", 1, "100.00"));
        venda(dia(2, 20, 0), "50.00", item("a", "A", 1, "50.00"));
        venda(dia(4, 23, 30), "70.00", item("a", "A", 1, "70.00"));

        List<VendaDiaDto> r = service.vendasPorDia(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), null);

        assertThat(r).hasSize(5);
        assertThat(r.get(0).data()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(r.get(0).quantidade()).isZero();
        assertThat(r.get(0).faturamento()).isEqualByComparingTo("0");
        assertThat(r.get(1).faturamento()).isEqualByComparingTo("150.00");
        assertThat(r.get(1).quantidade()).isEqualTo(2);
        assertThat(r.get(3).data()).isEqualTo(LocalDate.of(2026, 11, 4));
        assertThat(r.get(3).faturamento()).isEqualByComparingTo("70.00");
        assertThat(r.stream().map(VendaDiaDto::faturamento).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("220.00");
    }

    @Test
    void pedidosPorStatus_traz7StatusIncluindoCancelado() {
        venda(dia(2, 12, 0), "10.00", item("a", "A", 1, "10.00"));
        venda(dia(2, 13, 0), "10.00", item("a", "A", 1, "10.00"));
        pedido(StatusPedido.CANCELADO, dia(2, 14, 0), TipoAtendimento.ENTREGA, "10.00", "10.00", item("a", "A", 1, "10.00"));
        pedido(StatusPedido.PRONTO, dia(2, 15, 0), TipoAtendimento.ENTREGA, "10.00", "10.00", item("a", "A", 1, "10.00"));

        List<StatusPedido> ordem = List.of(StatusPedido.values());
        List<StatusQuantidadeDto> r = service.pedidosPorStatus(null, null, null);

        assertThat(r).hasSize(7);
        assertThat(r.stream().map(StatusQuantidadeDto::status).toList()).containsExactlyElementsOf(ordem);
        assertThat(r).filteredOn(s -> s.status() == StatusPedido.FINALIZADO).first()
                .satisfies(s -> {
                    assertThat(s.quantidade()).isEqualTo(2);
                    assertThat(s.descricao()).isEqualTo("Finalizado");
                });
        assertThat(r).filteredOn(s -> s.status() == StatusPedido.CANCELADO).first()
                .satisfies(s -> assertThat(s.quantidade()).isEqualTo(1));
        assertThat(r).filteredOn(s -> s.status() == StatusPedido.RECEBIDO).first()
                .satisfies(s -> assertThat(s.quantidade()).isZero());
        assertThat(r.stream().mapToLong(StatusQuantidadeDto::quantidade).sum()).isEqualTo(4);
    }

    // ---------- produtos ----------

    @Test
    void produtos_rateioDoDescontoEUltimoItemAbsorveCentavos() {
        // bruto 100.00 (33.33 + 33.33 + 33.34), total 90.00 (10% de desconto)
        pedido(StatusPedido.FINALIZADO, dia(5, 12, 0), TipoAtendimento.ENTREGA, "100.00", "90.00",
                item("a", "Alfa", 1, "33.33"), item("b", "Beta", 1, "33.33"), item("c", "Gama", 1, "33.34"));

        List<ProdutoRankingDto> r = service.produtos(null, null, null, null, null, null);

        assertThat(r).hasSize(3);
        BigDecimal soma = r.stream().map(ProdutoRankingDto::receita).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma).isEqualByComparingTo("90.00");
        // a = 33.33 * 0.9 = 29.997 -> 30.00; b idem; c absorve o resto = 30.00
        assertThat(r).extracting(ProdutoRankingDto::receita).allSatisfy(v -> assertThat(v).isEqualByComparingTo("30.00"));
        assertThat(r.get(0).percentualDoFaturamento()).isEqualByComparingTo("33.33");
    }

    @Test
    void produtos_rateioComDescontoProporcional() {
        pedido(StatusPedido.FINALIZADO, dia(5, 12, 0), TipoAtendimento.ENTREGA, "100.00", "80.00",
                item("a", "Alfa", 1, "75.00"), item("b", "Beta", 1, "25.00"));

        List<ProdutoRankingDto> r = service.produtos(null, null, null, null, null, null);

        assertThat(r.get(0).nome()).isEqualTo("Alfa");
        assertThat(r.get(0).receita()).isEqualByComparingTo("60.00");
        assertThat(r.get(1).receita()).isEqualByComparingTo("20.00");
        assertThat(r.get(0).percentualDoFaturamento()).isEqualByComparingTo("75.00");
    }

    private void cenarioRanking() {
        // Alfa: receita 100 qtd 1 | Beta: 60 qtd 6 | Gama: 60 qtd 2 | Delta: 10 qtd 10
        venda(dia(5, 12, 0), "100.00", item("a", "Alfa", 1, "100.00"));
        venda(dia(5, 13, 0), "60.00", item("b", "Beta", 6, "60.00"));
        venda(dia(5, 14, 0), "60.00", item("g", "Gama", 2, "60.00"));
        venda(dia(5, 15, 0), "10.00", item("d", "Delta", 10, "10.00"));
        pedido(StatusPedido.CANCELADO, dia(5, 16, 0), TipoAtendimento.ENTREGA, "999.00", "999.00",
                item("z", "Cancelado", 9, "999.00"));
    }

    private static List<String> nomes(List<ProdutoRankingDto> l) {
        return l.stream().map(ProdutoRankingDto::nome).toList();
    }

    @Test
    void produtos_porReceitaMaiorComDesempatePorNome() {
        cenarioRanking();
        assertThat(nomes(service.produtos(null, null, null, OrdenarPor.RECEITA, Ordem.MAIOR, 10)))
                .containsExactly("Alfa", "Beta", "Gama", "Delta");
    }

    @Test
    void produtos_porReceitaMenor() {
        cenarioRanking();
        assertThat(nomes(service.produtos(null, null, null, OrdenarPor.RECEITA, Ordem.MENOR, 10)))
                .containsExactly("Delta", "Beta", "Gama", "Alfa");
    }

    @Test
    void produtos_porQuantidadeMaiorEMenor() {
        cenarioRanking();
        assertThat(nomes(service.produtos(null, null, null, OrdenarPor.QUANTIDADE, Ordem.MAIOR, 10)))
                .containsExactly("Delta", "Beta", "Gama", "Alfa");
        assertThat(nomes(service.produtos(null, null, null, OrdenarPor.QUANTIDADE, Ordem.MENOR, 10)))
                .containsExactly("Alfa", "Gama", "Beta", "Delta");
    }

    @Test
    void produtos_respeitaLimiteEPadroes() {
        cenarioRanking();
        List<ProdutoRankingDto> r = service.produtos(null, null, null, null, null, 2);
        assertThat(nomes(r)).containsExactly("Alfa", "Beta");
        assertThat(r.get(0).quantidadeVendida()).isEqualTo(1);
        assertThat(service.produtos(null, null, null, null, null, null)).hasSize(4);
    }

    @Test
    void produtos_semVendaNaoListaProdutoCancelado() {
        cenarioRanking();
        assertThat(nomes(service.produtos(null, null, null, null, null, 20))).doesNotContain("Cancelado");
    }

    // ---------- picos ----------

    @Test
    void picosHorarios_24PosicoesEPicoCorreto() {
        venda(dia(5, 20, 0), "10.00", item("a", "A", 1, "10.00"));
        venda(dia(6, 20, 30), "10.00", item("a", "A", 1, "10.00"));
        venda(dia(6, 12, 0), "500.00", item("a", "A", 1, "500.00"));
        pedido(StatusPedido.CANCELADO, dia(6, 3, 0), TipoAtendimento.ENTREGA, "10.00", "10.00", item("a", "A", 1, "10.00"));

        PicoHorarioResponse r = service.picosHorarios(null, null, null);

        assertThat(r.itens()).hasSize(24);
        assertThat(r.itens().get(0).hora()).isZero();
        assertThat(r.itens().get(23).hora()).isEqualTo(23);
        assertThat(r.horaPico()).isEqualTo(20);
        assertThat(r.itens().get(20).quantidade()).isEqualTo(2);
        assertThat(r.itens().get(20).faturamento()).isEqualByComparingTo("20.00");
        assertThat(r.itens().get(3).quantidade()).isZero();
    }

    @Test
    void picosHorarios_pedidoAs2330ContaNaHora23() {
        venda(dia(5, 23, 30), "10.00", item("a", "A", 1, "10.00"));
        assertThat(service.picosHorarios(null, null, null).horaPico()).isEqualTo(23);
    }

    @Test
    void picosHorarios_semVendasPicoNulo() {
        PicoHorarioResponse r = service.picosHorarios(null, null, null);
        assertThat(r.horaPico()).isNull();
        assertThat(r.itens()).hasSize(24);
    }

    @Test
    void picosDiasSemana_7PosicoesDeSegundaADomingoEPicoCorreto() {
        // 2026-11-07 = sábado; 2026-11-02 = segunda
        venda(dia(7, 12, 0), "10.00", item("a", "A", 1, "10.00"));
        venda(dia(7, 13, 0), "10.00", item("a", "A", 1, "10.00"));
        venda(dia(2, 12, 0), "100.00", item("a", "A", 1, "100.00"));

        PicoDiaSemanaResponse r = service.picosDiasSemana(null, null, null);

        assertThat(r.itens()).hasSize(7);
        assertThat(r.itens().get(0).dia()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(r.itens().get(0).nome()).isEqualTo("Segunda-feira");
        assertThat(r.itens().get(6).dia()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(r.itens().get(6).nome()).isEqualTo("Domingo");
        assertThat(r.diaPico()).isEqualTo(DayOfWeek.SATURDAY);
        assertThat(r.itens().get(5).quantidade()).isEqualTo(2);
        assertThat(r.itens().get(5).faturamento()).isEqualByComparingTo("20.00");
    }

    @Test
    void picosDiasSemana_empatePorQuantidadeDecidePorFaturamento() {
        venda(dia(7, 12, 0), "10.00", item("a", "A", 1, "10.00")); // sábado
        venda(dia(2, 12, 0), "100.00", item("a", "A", 1, "100.00")); // segunda
        assertThat(service.picosDiasSemana(null, null, null).diaPico()).isEqualTo(DayOfWeek.MONDAY);
    }

    // ---------- últimos pedidos ----------

    private Pedido finalizado(String id, LocalDateTime entrada, LocalDateTime finalizadoEm, String valor,
            TipoAtendimento tipo, int qtd) {
        Pedido p = new Pedido();
        p.setId(id);
        p.setStatus(StatusPedido.FINALIZADO);
        p.setDataEntrada(entrada);
        p.setTipoAtendimento(tipo);
        p.setValorBruto(new BigDecimal(valor));
        p.setValorTotal(new BigDecimal(valor));
        p.setItens(new ArrayList<>(List.of(item("a", "A", qtd, valor))));
        p.registrarHistorico(StatusPedido.RECEBIDO, entrada, "u");
        p.registrarHistorico(StatusPedido.FINALIZADO, finalizadoEm, "u");
        finalizadosRecentes.add(p);
        return p;
    }

    @Test
    void ultimosPedidos_cincoMaisRecentesPorDataDeFinalizacaoComDiferencaCorreta() {
        // ticket 30d: dois pedidos de 100 e 200 -> 150.00
        base.add(finalizado("aaaaaaaa1111", dia(1, 12, 0), dia(1, 13, 0), "100.00", TipoAtendimento.ENTREGA, 1));
        base.add(finalizado("bbbbbbbb2222", dia(2, 12, 0), dia(2, 13, 0), "200.00", TipoAtendimento.ENTREGA, 1));
        // entrada mais antiga, mas finalizado depois: precisa vir primeiro
        finalizado("cccccccc3333", dia(1, 8, 0), dia(8, 11, 0), "165.00", TipoAtendimento.RETIRADA, 3);
        finalizado("dddddddd4444", dia(3, 12, 0), dia(3, 13, 0), "10.00", TipoAtendimento.ENTREGA, 1);
        finalizado("eeeeeeee5555", dia(4, 12, 0), dia(4, 13, 0), "10.00", TipoAtendimento.ENTREGA, 1);
        finalizado("ffffffff6666", dia(5, 12, 0), dia(5, 13, 0), "10.00", TipoAtendimento.ENTREGA, 1);
        finalizado("gggggggg7777", dia(6, 12, 0), dia(6, 13, 0), "10.00", TipoAtendimento.ENTREGA, 1);

        List<UltimoPedidoDto> r = service.ultimosPedidos(null);

        assertThat(r).hasSize(5);
        assertThat(r.stream().map(UltimoPedidoDto::idCurto))
                .containsExactly("cccccccc", "gggggggg", "ffffffff", "eeeeeeee", "dddddddd");
        UltimoPedidoDto primeiro = r.get(0);
        assertThat(primeiro.id()).isEqualTo("cccccccc3333");
        assertThat(primeiro.dataFinalizacao()).isEqualTo(dia(8, 11, 0));
        assertThat(primeiro.quantidadeItens()).isEqualTo(3);
        assertThat(primeiro.valorTotal()).isEqualByComparingTo("165.00");
        assertThat(primeiro.tipoAtendimento()).isEqualTo(TipoAtendimento.RETIRADA);
        // ticket médio dos 30 dias = (100 + 200) / 2 = 150
        assertThat(primeiro.diferencaTicketMedio()).isEqualByComparingTo("15.00");
        assertThat(primeiro.diferencaTicketMedioPercentual()).isEqualByComparingTo("10.00");
        assertThat(r.get(1).diferencaTicketMedio()).isEqualByComparingTo("-140.00");
    }

    @Test
    void ultimosPedidos_respeitaTipoAtendimentoEIgnoraPedidoSemFinalizacaoNoHistorico() {
        base.add(finalizado("aaaaaaaa1111", dia(1, 12, 0), dia(1, 13, 0), "100.00", TipoAtendimento.ENTREGA, 1));
        finalizado("bbbbbbbb2222", dia(2, 12, 0), dia(2, 13, 0), "50.00", TipoAtendimento.RETIRADA, 1);
        Pedido semHistorico = finalizado("cccccccc3333", dia(3, 12, 0), dia(3, 13, 0), "50.00", TipoAtendimento.RETIRADA, 1);
        semHistorico.setHistoricoStatus(new ArrayList<>());

        List<UltimoPedidoDto> r = service.ultimosPedidos(TipoAtendimento.RETIRADA);

        assertThat(r).extracting(UltimoPedidoDto::id).containsExactly("bbbbbbbb2222");
    }

    @Test
    void ultimosPedidos_semVendasNosUltimos30DiasDiferencaZero() {
        finalizado("aaaaaaaa1111", dia(1, 12, 0), dia(1, 13, 0), "80.00", TipoAtendimento.ENTREGA, 1);
        UltimoPedidoDto r = service.ultimosPedidos(null).get(0);
        assertThat(r.diferencaTicketMedio()).isEqualByComparingTo("80.00");
        assertThat(r.diferencaTicketMedioPercentual()).isEqualByComparingTo("0");
    }
}
