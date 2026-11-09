package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.dashboard.Ordem;
import com.umc.pediupatrao.dto.dashboard.OrdenarPor;
import com.umc.pediupatrao.dto.dashboard.PeriodoDto;
import com.umc.pediupatrao.dto.dashboard.PicoDiaSemanaResponse;
import com.umc.pediupatrao.dto.dashboard.PicoHorarioResponse;
import com.umc.pediupatrao.dto.dashboard.ProdutoRankingDto;
import com.umc.pediupatrao.dto.dashboard.ResumoDashboardResponse;
import com.umc.pediupatrao.dto.dashboard.StatusQuantidadeDto;
import com.umc.pediupatrao.dto.dashboard.UltimoPedidoDto;
import com.umc.pediupatrao.dto.dashboard.VendaDiaDto;
import com.umc.pediupatrao.entity.HistoricoStatus;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    public static final int MAX_PERIOD_DAYS = 366;
    public static final int DIAS_PADRAO = 30;
    public static final int LIMITE_MIN = 1;
    public static final int LIMITE_MAX = 20;
    public static final int QTD_ULTIMOS = 5;

    private static final int CASAS = CalculoPedido.CASAS;
    private static final RoundingMode ARRED = CalculoPedido.ARREDONDAMENTO;
    private static final int ESCALA_INTERMEDIARIA = 10;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(CASAS);

    private static final Map<DayOfWeek, String> NOMES_DIAS = Map.of(
            DayOfWeek.MONDAY, "Segunda-feira",
            DayOfWeek.TUESDAY, "Terça-feira",
            DayOfWeek.WEDNESDAY, "Quarta-feira",
            DayOfWeek.THURSDAY, "Quinta-feira",
            DayOfWeek.FRIDAY, "Sexta-feira",
            DayOfWeek.SATURDAY, "Sábado",
            DayOfWeek.SUNDAY, "Domingo");

    private final PedidoRepository pedidoRepository;
    private final Clock clock;

    public DashboardService(PedidoRepository pedidoRepository, Clock clock) {
        this.pedidoRepository = pedidoRepository;
        this.clock = clock;
    }

    public ResumoDashboardResponse resumo(LocalDate inicio, LocalDate fim, TipoAtendimento tipo) {
        PeriodoDto periodo = resolverPeriodo(inicio, fim);
        List<Pedido> pedidos = carregar(periodo, tipo);
        List<Pedido> vendas = vendas(pedidos);
        BigDecimal faturamento = faturamento(vendas);
        BigDecimal descontos = vendas.stream()
                .map(p -> p.getValorBruto() == null ? BigDecimal.ZERO : p.getValorBruto().subtract(p.getValorTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(CASAS, ARRED);
        long cancelados = pedidos.stream().filter(p -> p.getStatus() == StatusPedido.CANCELADO).count();
        return new ResumoDashboardResponse(periodo, faturamento, vendas.size(),
                ticketMedio(faturamento, vendas.size()), cancelados, descontos);
    }

    public List<VendaDiaDto> vendasPorDia(LocalDate inicio, LocalDate fim, TipoAtendimento tipo) {
        PeriodoDto periodo = resolverPeriodo(inicio, fim);
        Map<LocalDate, List<Pedido>> porDia = vendas(carregar(periodo, tipo)).stream()
                .collect(Collectors.groupingBy(p -> p.getDataEntrada().toLocalDate()));
        List<VendaDiaDto> resultado = new ArrayList<>();
        for (LocalDate dia = periodo.inicio(); !dia.isAfter(periodo.fim()); dia = dia.plusDays(1)) {
            List<Pedido> doDia = porDia.getOrDefault(dia, List.of());
            resultado.add(new VendaDiaDto(dia, faturamento(doDia), doDia.size()));
        }
        return resultado;
    }

    public List<StatusQuantidadeDto> pedidosPorStatus(LocalDate inicio, LocalDate fim, TipoAtendimento tipo) {
        PeriodoDto periodo = resolverPeriodo(inicio, fim);
        Map<StatusPedido, Long> contagem = carregar(periodo, tipo).stream()
                .filter(p -> p.getStatus() != null)
                .collect(Collectors.groupingBy(Pedido::getStatus, Collectors.counting()));
        List<StatusQuantidadeDto> resultado = new ArrayList<>();
        for (StatusPedido s : StatusPedido.values()) {
            resultado.add(new StatusQuantidadeDto(s, s.getDescricao(), contagem.getOrDefault(s, 0L)));
        }
        return resultado;
    }

    public List<ProdutoRankingDto> produtos(LocalDate inicio, LocalDate fim, TipoAtendimento tipo,
            OrdenarPor ordenarPor, Ordem ordem, Integer limite) {
        PeriodoDto periodo = resolverPeriodo(inicio, fim);
        OrdenarPor criterio = ordenarPor == null ? OrdenarPor.RECEITA : ordenarPor;
        Ordem direcao = ordem == null ? Ordem.MAIOR : ordem;
        int max = limite == null ? 10 : limite;
        if (max < LIMITE_MIN || max > LIMITE_MAX) {
            throw new IllegalArgumentException("limite deve estar entre " + LIMITE_MIN + " e " + LIMITE_MAX + ".");
        }

        List<Pedido> vendas = vendas(carregar(periodo, tipo));
        BigDecimal faturamento = faturamento(vendas);

        Map<String, Acumulador> porProduto = new LinkedHashMap<>();
        for (Pedido venda : vendas) {
            List<ItemPedido> itens = venda.getItens();
            List<BigDecimal> receitas = ratear(venda);
            for (int i = 0; i < itens.size(); i++) {
                ItemPedido item = itens.get(i);
                Acumulador acc = porProduto.computeIfAbsent(String.valueOf(item.getProdutoId()),
                        k -> new Acumulador(item.getProdutoId(), item.getNomeProduto()));
                acc.quantidade += item.getQuantidade() == null ? 0 : item.getQuantidade();
                acc.receita = acc.receita.add(receitas.get(i));
            }
        }

        Comparator<Acumulador> porNome = Comparator
                .comparing((Acumulador a) -> a.nome == null ? "" : a.nome, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(a -> a.produtoId == null ? "" : a.produtoId);
        Comparator<Acumulador> principal = criterio == OrdenarPor.RECEITA
                ? Comparator.comparing((Acumulador a) -> a.receita)
                : Comparator.comparingLong((Acumulador a) -> a.quantidade);
        if (direcao == Ordem.MAIOR) {
            principal = principal.reversed();
        }

        return porProduto.values().stream()
                .sorted(principal.thenComparing(porNome))
                .limit(max)
                .map(a -> new ProdutoRankingDto(a.produtoId, a.nome, a.quantidade,
                        a.receita.setScale(CASAS, ARRED), percentual(a.receita, faturamento)))
                .toList();
    }

    public PicoHorarioResponse picosHorarios(LocalDate inicio, LocalDate fim, TipoAtendimento tipo) {
        PeriodoDto periodo = resolverPeriodo(inicio, fim);
        Map<Integer, List<Pedido>> porHora = vendas(carregar(periodo, tipo)).stream()
                .collect(Collectors.groupingBy(p -> p.getDataEntrada().getHour()));
        List<PicoHorarioResponse.Hora> itens = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            List<Pedido> doGrupo = porHora.getOrDefault(h, List.of());
            itens.add(new PicoHorarioResponse.Hora(h, doGrupo.size(), faturamento(doGrupo)));
        }
        Integer pico = itens.stream()
                .filter(i -> i.quantidade() > 0)
                .max(Comparator.comparingLong(PicoHorarioResponse.Hora::quantidade)
                        .thenComparing(PicoHorarioResponse.Hora::faturamento)
                        .thenComparing(Comparator.comparingInt(PicoHorarioResponse.Hora::hora).reversed()))
                .map(PicoHorarioResponse.Hora::hora)
                .orElse(null);
        return new PicoHorarioResponse(pico, itens);
    }

    public PicoDiaSemanaResponse picosDiasSemana(LocalDate inicio, LocalDate fim, TipoAtendimento tipo) {
        PeriodoDto periodo = resolverPeriodo(inicio, fim);
        Map<DayOfWeek, List<Pedido>> porDia = new EnumMap<>(DayOfWeek.class);
        vendas(carregar(periodo, tipo)).forEach(
                p -> porDia.computeIfAbsent(p.getDataEntrada().getDayOfWeek(), k -> new ArrayList<>()).add(p));
        List<PicoDiaSemanaResponse.Dia> itens = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            List<Pedido> doGrupo = porDia.getOrDefault(d, List.of());
            itens.add(new PicoDiaSemanaResponse.Dia(d, NOMES_DIAS.get(d), doGrupo.size(), faturamento(doGrupo)));
        }
        DayOfWeek pico = itens.stream()
                .filter(i -> i.quantidade() > 0)
                .max(Comparator.comparingLong(PicoDiaSemanaResponse.Dia::quantidade)
                        .thenComparing(PicoDiaSemanaResponse.Dia::faturamento)
                        .thenComparing(Comparator.comparing(PicoDiaSemanaResponse.Dia::dia).reversed()))
                .map(PicoDiaSemanaResponse.Dia::dia)
                .orElse(null);
        return new PicoDiaSemanaResponse(pico, itens);
    }

    public List<UltimoPedidoDto> ultimosPedidos(TipoAtendimento tipo) {
        LocalDate hoje = LocalDate.now(clock);
        PeriodoDto ultimos30 = new PeriodoDto(hoje.minusDays(DIAS_PADRAO - 1L), hoje);
        List<Pedido> vendas30 = vendas(carregar(ultimos30, tipo));
        BigDecimal ticket = ticketMedio(faturamento(vendas30), vendas30.size());

        record ComData(Pedido pedido, LocalDateTime finalizadoEm) {
        }
        return pedidoRepository.findTop100ByStatusOrderByDataEntradaDesc(StatusPedido.FINALIZADO).stream()
                .filter(DashboardService::valido)
                .filter(p -> tipo == null || p.getTipoAtendimento() == tipo)
                .map(p -> new ComData(p, dataFinalizacao(p).orElse(null)))
                .filter(c -> c.finalizadoEm() != null)
                .sorted(Comparator.comparing(ComData::finalizadoEm).reversed()
                        .thenComparing(c -> String.valueOf(c.pedido().getId()), Comparator.reverseOrder()))
                .limit(QTD_ULTIMOS)
                .map(c -> {
                    Pedido p = c.pedido();
                    BigDecimal valor = p.getValorTotal().setScale(CASAS, ARRED);
                    BigDecimal dif = valor.subtract(ticket).setScale(CASAS, ARRED);
                    String id = String.valueOf(p.getId());
                    return new UltimoPedidoDto(p.getId(), id.length() > 8 ? id.substring(0, 8) : id,
                            c.finalizadoEm(), p.getTipoAtendimento(), quantidadeItens(p), valor, dif,
                            percentual(dif, ticket));
                })
                .toList();
    }

    private PeriodoDto resolverPeriodo(LocalDate inicio, LocalDate fim) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate f = fim != null ? fim : hoje;
        LocalDate i = inicio != null ? inicio : f.minusDays(DIAS_PADRAO - 1L);
        if (f.isAfter(hoje)) {
            throw new IllegalArgumentException("A data final não pode estar no futuro.");
        }
        if (i.isAfter(f)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }
        if (ChronoUnit.DAYS.between(i, f) + 1 > MAX_PERIOD_DAYS) {
            throw new IllegalArgumentException("O período não pode exceder " + MAX_PERIOD_DAYS + " dias.");
        }
        return new PeriodoDto(i, f);
    }

    private List<Pedido> carregar(PeriodoDto periodo, TipoAtendimento tipo) {
        LocalDateTime de = periodo.inicio().atStartOfDay();
        LocalDateTime ate = periodo.fim().plusDays(1).atStartOfDay().minusNanos(1);
        return pedidoRepository.findByDataEntradaBetween(de, ate).stream()
                .filter(DashboardService::valido)
                .filter(p -> p.getDataEntrada() != null)
                .filter(p -> tipo == null || p.getTipoAtendimento() == tipo)
                .toList();
    }

    private static boolean valido(Pedido p) {
        return p.getItens() != null && !p.getItens().isEmpty() && p.getValorTotal() != null;
    }

    private static List<Pedido> vendas(List<Pedido> pedidos) {
        return pedidos.stream().filter(p -> p.getStatus() == StatusPedido.FINALIZADO).toList();
    }

    private static BigDecimal faturamento(List<Pedido> vendas) {
        return vendas.stream().map(Pedido::getValorTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(CASAS, ARRED);
    }

    private static BigDecimal ticketMedio(BigDecimal faturamento, int quantidade) {
        if (quantidade == 0) {
            return ZERO;
        }
        return faturamento.divide(BigDecimal.valueOf(quantidade), CASAS, ARRED);
    }

    private static BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return ZERO;
        }
        return parte.multiply(CEM).divide(total, CASAS, ARRED);
    }

    private static List<BigDecimal> ratear(Pedido venda) {
        List<ItemPedido> itens = venda.getItens();
        BigDecimal bruto = venda.getValorBruto();
        List<BigDecimal> receitas = new ArrayList<>();
        if (bruto == null || bruto.signum() == 0) {
            itens.forEach(i -> receitas.add(subtotal(i).setScale(CASAS, ARRED)));
            return receitas;
        }
        BigDecimal acumulado = BigDecimal.ZERO;
        for (int i = 0; i < itens.size() - 1; i++) {
            BigDecimal r = subtotal(itens.get(i)).multiply(venda.getValorTotal())
                    .divide(bruto, ESCALA_INTERMEDIARIA, ARRED).setScale(CASAS, ARRED);
            receitas.add(r);
            acumulado = acumulado.add(r);
        }
        receitas.add(venda.getValorTotal().subtract(acumulado).setScale(CASAS, ARRED));
        return receitas;
    }

    private static BigDecimal subtotal(ItemPedido item) {
        return item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal();
    }

    private static int quantidadeItens(Pedido p) {
        return p.getItens().stream().mapToInt(i -> i.getQuantidade() == null ? 0 : i.getQuantidade()).sum();
    }

    private static Optional<LocalDateTime> dataFinalizacao(Pedido p) {
        if (p.getHistoricoStatus() == null) {
            return Optional.empty();
        }
        return p.getHistoricoStatus().stream()
                .filter(h -> h.getStatus() == StatusPedido.FINALIZADO && h.getDataHora() != null)
                .map(HistoricoStatus::getDataHora)
                .max(Comparator.naturalOrder());
    }

    private static final class Acumulador {
        final String produtoId;
        final String nome;
        long quantidade;
        BigDecimal receita = BigDecimal.ZERO;

        Acumulador(String produtoId, String nome) {
            this.produtoId = produtoId;
            this.nome = nome;
        }
    }
}
