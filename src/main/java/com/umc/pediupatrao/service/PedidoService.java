package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.EntidadeAuditada;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.OperacaoAuditoria;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PedidoService {
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository, AuditoriaService auditoriaService, Clock relogio) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Pedido registrar(PedidoRequest request) {
        validarEstrutura(request);
        if (!clienteRepository.existsById(request.getClienteId())) {
            throw new IllegalArgumentException("Cliente não encontrado.");
        }

        List<ItemPedido> itens = new ArrayList<>();
        for (ItemPedidoRequest item : request.getItens()) {
            Produto produto = produtoVendavel(item.getProdutoId());
            BigDecimal preco = CalculoPedido.preco(produto.getPreco());
            itens.add(new ItemPedido(produto.getId(), produto.getNome(), item.getQuantidade(),
                    preco, CalculoPedido.subtotal(preco, item.getQuantidade())));
        }

        String responsavel = UsuarioLogado.exigirUsername();
        LocalDateTime agora = LocalDateTime.now(relogio);

        Pedido pedido = new Pedido();
        pedido.setClienteId(request.getClienteId());
        pedido.setTipoAtendimento(request.getTipoAtendimento());
        pedido.setItens(itens);
        pedido.setValorBruto(CalculoPedido.valorBruto(itens));
        pedido.setValorTotal(CalculoPedido.valorTotal(pedido.getValorBruto(), null));
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setCriadoPor(responsavel);
        pedido.setDataEntrada(agora);
        pedido.registrarHistorico(StatusPedido.RECEBIDO, agora, responsavel);

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(OperacaoAuditoria.CRIACAO_PEDIDO, EntidadeAuditada.PEDIDO, salvo.getId(),
                resumoDaCriacao(salvo), null);
        return salvo;
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public Pedido buscar(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado."));
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public Map<String, String> nomesDosClientes(Collection<Pedido> pedidos) {
        List<String> ids = pedidos.stream().map(Pedido::getClienteId).filter(Objects::nonNull).distinct().toList();
        return clienteRepository.findAllById(ids).stream()
                .filter(c -> c.getNome() != null)
                .collect(Collectors.toMap(Cliente::getId, Cliente::getNome, (a, b) -> a));
    }

    private void validarEstrutura(PedidoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dados do pedido não informados.");
        }
        if (request.getClienteId() == null || request.getClienteId().isBlank()) {
            throw new IllegalArgumentException("Informe o cliente.");
        }
        if (request.getTipoAtendimento() == null) {
            throw new IllegalArgumentException("Informe o tipo de atendimento.");
        }
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new IllegalArgumentException("O pedido precisa de pelo menos um item.");
        }
        for (ItemPedidoRequest item : request.getItens()) {
            if (item == null || item.getProdutoId() == null || item.getProdutoId().isBlank()) {
                throw new IllegalArgumentException("Informe o produto de cada item.");
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
            }
            if (item.getQuantidade() > ItemPedidoRequest.QUANTIDADE_MAXIMA) {
                throw new IllegalArgumentException("Quantidade acima do permitido por item.");
            }
        }
    }

    private Produto produtoVendavel(String produtoId) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + produtoId));
        if (!Boolean.TRUE.equals(produto.getAtivo())) {
            throw new IllegalArgumentException("Produto inativo: " + produto.getNome());
        }
        if (produto.getPreco() == null || produto.getPreco() <= 0) {
            throw new IllegalArgumentException("Produto sem preço válido: " + produto.getNome());
        }
        return produto;
    }

    private static List<AlteracaoCampo> resumoDaCriacao(Pedido pedido) {
        String itens = pedido.getItens().stream()
                .map(i -> i.getQuantidade() + "x " + i.getNomeProduto() + " (" + i.getPrecoUnitario().toPlainString() + ")")
                .collect(Collectors.joining("; "));
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("clienteId", pedido.getClienteId());
        campos.put("tipoAtendimento", pedido.getTipoAtendimento());
        campos.put("itens", itens);
        campos.put("valorBruto", pedido.getValorBruto());
        campos.put("valorTotal", pedido.getValorTotal());
        campos.put("status", pedido.getStatus());
        campos.put("criadoPor", pedido.getCriadoPor());
        campos.put("dataEntrada", pedido.getDataEntrada());
        return campos.entrySet().stream()
                .map(e -> new AlteracaoCampo(e.getKey(), null, ComparadorCampos.formatar(e.getValue())))
                .toList();
    }
}
