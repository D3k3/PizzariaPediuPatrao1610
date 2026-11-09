package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.service.CalculoPedido;
import com.umc.pediupatrao.service.FluxoStatusPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Dados de demonstração para o dashboard. Só existe no perfil "local" e só grava pedidos se a coleção
 * estiver vazia. Monta os documentos diretamente (sem serviços, sem SecurityContext) e, por isso,
 * não gera nenhum evento de auditoria.
 */
@Component
@Profile("local")
@Order(30)
public class DashboardSeed implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DashboardSeed.class);

    static final int TOTAL_PEDIDOS = 120;
    static final int DIAS = 45;
    static final int PEDIDOS_CANCELADOS = 10;   // ~8%
    static final int PEDIDOS_EM_ANDAMENTO = 20; // ~17%; o restante (90, 75%) é FINALIZADO
    private static final long SEMENTE = 20260601L;
    private static final String ATENDENTE = "atendente";
    private static final String GERENTE = "gerente";
    private static final BigDecimal[] DESCONTOS = {
            BigDecimal.valueOf(5), BigDecimal.valueOf(10), BigDecimal.valueOf(15), BigDecimal.valueOf(20) };
    private static final String[] MOTIVOS = {
            "Cliente desistiu do pedido (demonstração)", "Pedido duplicado (demonstração)",
            "Endereço fora da área de entrega (demonstração)" };
    private static final String[][] CATALOGO = {
            { "Pizza Margherita (demo)", "Pizza", "42.90" },
            { "Pizza Calabresa (demo)", "Pizza", "46.90" },
            { "Pizza Portuguesa (demo)", "Pizza", "52.90" },
            { "Pizza Quatro Queijos (demo)", "Pizza", "54.90" },
            { "Pizza Frango com Catupiry (demo)", "Pizza", "49.90" },
            { "Pizza Chocolate (demo)", "Pizza", "44.90" },
            { "Refrigerante 2L (demo)", "Bebida", "12.00" },
            { "Suco Natural 500ml (demo)", "Bebida", "9.50" },
            { "Cerveja Long Neck (demo)", "Bebida", "11.00" },
            { "Brownie com Sorvete (demo)", "Sobremesa", "18.00" } };

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final Clock relogio;

    public DashboardSeed(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository, Clock relogio) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.relogio = relogio;
    }

    @Override
    public void run(String... args) {
        if (pedidoRepository.count() > 0) {
            log.info("DashboardSeed: coleção 'pedidos' já tem dados; nada a fazer.");
            return;
        }
        List<Produto> produtos = produtosParaUso();
        List<Cliente> clientes = clientesParaUso();
        if (produtos.isEmpty() || clientes.isEmpty()) {
            log.warn("DashboardSeed: sem produtos ativos ou clientes; pedidos não criados.");
            return;
        }

        Random rnd = new Random(SEMENTE);
        LocalDateTime agora = LocalDateTime.now(relogio);
        List<Pedido> pedidos = new ArrayList<>(TOTAL_PEDIDOS);
        for (StatusPedido alvo : sortearStatusAlvo(rnd)) {
            pedidos.add(montarPedido(alvo, produtos, clientes, agora, rnd));
        }
        pedidoRepository.saveAll(pedidos);
        log.info("DashboardSeed: {} pedidos de demonstração criados.", pedidos.size());
    }

    private List<Produto> produtosParaUso() {
        if (produtoRepository.count() == 0) {
            List<Produto> novos = new ArrayList<>();
            for (String[] p : CATALOGO) {
                Produto produto = new Produto();
                produto.setNome(p[0]);
                produto.setTipo(p[1]);
                produto.setPreco(Double.valueOf(p[2]));
                produto.setAtivo(true);
                novos.add(produto);
            }
            produtoRepository.saveAll(novos);
            log.info("DashboardSeed: {} produtos de demonstração criados.", novos.size());
        }
        return produtoRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getAtivo()) && p.getPreco() != null && p.getPreco() > 0)
                .toList();
    }

    private List<Cliente> clientesParaUso() {
        if (clienteRepository.count() == 0) {
            List<Cliente> novos = new ArrayList<>();
            for (int i = 1; i <= 8; i++) {
                Cliente c = new Cliente();
                c.setNome("Cliente Fictício " + i);
                c.setTelefone("(00) 00000-000" + i);
                c.setCep("00000-000");
                c.setLogradouro("Rua Exemplo Fictícia");
                c.setNumero(String.valueOf(i));
                c.setBairro("Bairro Demonstração");
                c.setCidade("Cidade Exemplo");
                c.setEstado("XX");
                c.setEndereco("Rua Exemplo Fictícia, " + i + " - Bairro Demonstração, Cidade Exemplo/XX");
                novos.add(c);
            }
            clienteRepository.saveAll(novos);
            log.info("DashboardSeed: {} clientes fictícios criados.", novos.size());
        }
        return clienteRepository.findAll();
    }

    private static List<StatusPedido> sortearStatusAlvo(Random rnd) {
        StatusPedido[] andamento = { StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO,
                StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.RETIRADO };
        List<StatusPedido> alvos = new ArrayList<>(TOTAL_PEDIDOS);
        for (int i = 0; i < TOTAL_PEDIDOS - PEDIDOS_CANCELADOS - PEDIDOS_EM_ANDAMENTO; i++) {
            alvos.add(StatusPedido.FINALIZADO);
        }
        for (int i = 0; i < PEDIDOS_CANCELADOS; i++) {
            alvos.add(StatusPedido.CANCELADO);
        }
        for (int i = 0; i < PEDIDOS_EM_ANDAMENTO; i++) {
            alvos.add(andamento[i % andamento.length]);
        }
        Collections.shuffle(alvos, rnd);
        return alvos;
    }

    private Pedido montarPedido(StatusPedido alvo, List<Produto> produtos, List<Cliente> clientes,
            LocalDateTime agora, Random rnd) {
        TipoAtendimento tipo = tipoPara(alvo, rnd);
        List<ItemPedido> itens = sortearItens(produtos, rnd);

        Pedido pedido = new Pedido();
        pedido.setClienteId(clientes.get(rnd.nextInt(clientes.size())).getId());
        pedido.setTipoAtendimento(tipo);
        pedido.setItens(itens);
        pedido.setValorBruto(CalculoPedido.valorBruto(itens));
        if (rnd.nextInt(100) < 20) {
            pedido.setPercentualDesconto(DESCONTOS[rnd.nextInt(DESCONTOS.length)]);
        }
        pedido.setValorTotal(CalculoPedido.valorTotal(pedido.getValorBruto(), pedido.getPercentualDesconto()));
        pedido.setCriadoPor(ATENDENTE);

        // Cadeia de status obedecendo exatamente o FluxoStatusPedido.
        StatusPedido ultimoDoFluxo = alvo == StatusPedido.CANCELADO ? sortearPontoDeCancelamento(rnd) : alvo;
        List<StatusPedido> cadeia = cadeiaAte(ultimoDoFluxo, tipo);
        int[] minutos = new int[cadeia.size()];
        int total = 0;
        for (int i = 1; i < cadeia.size(); i++) {
            minutos[i] = duracaoDaEtapa(cadeia.get(i), tipo, rnd);
            total += minutos[i];
        }
        int minutosCancelamento = alvo == StatusPedido.CANCELADO ? 3 + rnd.nextInt(15) : 0;
        total += minutosCancelamento;

        LocalDateTime entrada = alvo == StatusPedido.FINALIZADO || alvo == StatusPedido.CANCELADO
                ? sortearEntradaHistorica(agora, total, rnd)
                : agora.minusMinutes(total + 5 + rnd.nextInt(90));
        pedido.setDataEntrada(entrada);

        LocalDateTime momento = entrada;
        for (int i = 0; i < cadeia.size(); i++) {
            momento = momento.plusMinutes(minutos[i]);
            StatusPedido etapa = cadeia.get(i);
            pedido.registrarHistorico(etapa, momento, ATENDENTE);
            if (etapa.isSaida()) {
                pedido.setDataSaida(momento);
                pedido.setResponsavelSaida(ATENDENTE);
            }
        }
        if (alvo == StatusPedido.CANCELADO) {
            LocalDateTime quando = momento.plusMinutes(minutosCancelamento);
            pedido.registrarHistorico(StatusPedido.CANCELADO, quando, GERENTE);
            pedido.setJustificativaCancelamento(MOTIVOS[rnd.nextInt(MOTIVOS.length)]);
            pedido.setCanceladoPor(GERENTE);
            pedido.setDataCancelamento(quando);
        }
        pedido.setStatus(alvo);
        return pedido;
    }

    private static TipoAtendimento tipoPara(StatusPedido alvo, Random rnd) {
        if (alvo == StatusPedido.SAIU_PARA_ENTREGA) {
            return TipoAtendimento.ENTREGA;
        }
        if (alvo == StatusPedido.RETIRADO) {
            return TipoAtendimento.RETIRADA;
        }
        return rnd.nextInt(100) < 60 ? TipoAtendimento.ENTREGA : TipoAtendimento.RETIRADA;
    }

    /** Cancelamento só é permitido antes da saída (RECEBIDO, EM_PREPARACAO ou PRONTO). */
    private static StatusPedido sortearPontoDeCancelamento(Random rnd) {
        StatusPedido[] antes = { StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO };
        return antes[rnd.nextInt(antes.length)];
    }

    private static List<StatusPedido> cadeiaAte(StatusPedido ultimo, TipoAtendimento tipo) {
        List<StatusPedido> cadeia = new ArrayList<>();
        StatusPedido atual = StatusPedido.RECEBIDO;
        cadeia.add(atual);
        while (atual != ultimo) {
            List<StatusPedido> proximos = FluxoStatusPedido.proximos(atual, tipo);
            if (proximos.isEmpty()) {
                throw new IllegalStateException("Fluxo não alcança " + ultimo + " para " + tipo);
            }
            atual = proximos.get(0);
            cadeia.add(atual);
        }
        return cadeia;
    }

    private static int duracaoDaEtapa(StatusPedido etapa, TipoAtendimento tipo, Random rnd) {
        return switch (etapa) {
            case EM_PREPARACAO -> 3 + rnd.nextInt(8);
            case PRONTO -> 12 + rnd.nextInt(15);
            case SAIU_PARA_ENTREGA, RETIRADO -> 2 + rnd.nextInt(8);
            case FINALIZADO -> tipo == TipoAtendimento.ENTREGA ? 15 + rnd.nextInt(30) : 1 + rnd.nextInt(5);
            default -> 0;
        };
    }

    private static List<ItemPedido> sortearItens(List<Produto> produtos, Random rnd) {
        int quantos = Math.min(produtos.size(), 1 + rnd.nextInt(4));
        Set<Integer> indices = new LinkedHashSet<>();
        while (indices.size() < quantos) {
            // Distribuição enviesada: os primeiros produtos vendem mais, gerando um ranking com contraste.
            indices.add((int) (produtos.size() * Math.pow(rnd.nextDouble(), 1.8)));
        }
        List<ItemPedido> itens = new ArrayList<>();
        for (int indice : indices) {
            Produto p = produtos.get(indice);
            int quantidade = 1 + rnd.nextInt(3);
            BigDecimal preco = CalculoPedido.preco(p.getPreco());
            itens.add(new ItemPedido(p.getId(), p.getNome(), quantidade, preco,
                    CalculoPedido.subtotal(preco, quantidade)));
        }
        return itens;
    }

    /**
     * Sorteia o dia (sexta a domingo pesam o dobro) e o horário (concentrado em 11h-14h e 18h-22h) e garante
     * que a última etapa do pedido não ultrapasse o instante atual.
     */
    private static LocalDateTime sortearEntradaHistorica(LocalDateTime agora, int minutosDeDuracao, Random rnd) {
        LocalDate hoje = agora.toLocalDate();
        while (true) {
            LocalDate dia = sortearDia(hoje, rnd);
            LocalDateTime entrada = dia.atTime(sortearHorario(rnd));
            if (!entrada.plusMinutes(minutosDeDuracao).isAfter(agora)) {
                return entrada;
            }
        }
    }

    private static LocalDate sortearDia(LocalDate hoje, Random rnd) {
        int[] pesos = new int[DIAS];
        int soma = 0;
        for (int i = 0; i < DIAS; i++) {
            DayOfWeek dow = hoje.minusDays(i).getDayOfWeek();
            boolean fimDeSemana = dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
            pesos[i] = fimDeSemana ? 2 : 1;
            soma += pesos[i];
        }
        int sorteio = rnd.nextInt(soma);
        for (int i = 0; i < DIAS; i++) {
            sorteio -= pesos[i];
            if (sorteio < 0) {
                return hoje.minusDays(i);
            }
        }
        return hoje;
    }

    private static LocalTime sortearHorario(Random rnd) {
        int faixa = rnd.nextInt(100);
        int minutoDoDia;
        if (faixa < 40) {
            minutoDoDia = 11 * 60 + rnd.nextInt(3 * 60);        // 11:00-13:59
        } else if (faixa < 95) {
            minutoDoDia = 18 * 60 + rnd.nextInt(4 * 60);        // 18:00-21:59
        } else {
            minutoDoDia = 14 * 60 + rnd.nextInt(4 * 60);        // 14:00-17:59 (fora de pico)
        }
        return LocalTime.of(minutoDoDia / 60, minutoDoDia % 60, rnd.nextInt(60));
    }
}
