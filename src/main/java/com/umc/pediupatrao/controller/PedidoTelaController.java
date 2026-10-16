package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.FluxoStatusPedido;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Controller
public class PedidoTelaController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;

    public PedidoTelaController(PedidoService pedidoService, ClienteService clienteService,
            ProdutoService produtoService) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
    }

    @GetMapping("/pedidos")
    public String listar(Model model) {
        List<Pedido> pedidos = pedidoService.listarPedidos().stream()
                .sorted(Comparator.comparing(Pedido::getDataEntrada, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        Map<String, List<StatusPedido>> proximosStatus = new HashMap<>();
        pedidos.forEach(p -> proximosStatus.put(p.getId(),
                FluxoStatusPedido.proximos(p.getStatus(), p.getTipoAtendimento())));

        model.addAttribute("pedidos", pedidos);
        model.addAttribute("nomesClientes", pedidoService.nomesDosClientes(pedidos));
        model.addAttribute("proximosStatus", proximosStatus);
        model.addAttribute("content", "pedidos :: content");
        return "layout";
    }

    @GetMapping("/pedidos/novo")
    public String novo(Model model) {
        PedidoRequest form = new PedidoRequest();
        form.getItens().add(new ItemPedidoRequest());
        model.addAttribute("pedidoForm", form);
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("produtos", produtoService.listarAtivos());
        model.addAttribute("tipos", TipoAtendimento.values());
        model.addAttribute("content", "pedidos/form :: content");
        return "layout";
    }

    @PostMapping("/pedidos/salvar")
    public String salvar(@ModelAttribute("pedidoForm") PedidoRequest form, RedirectAttributes redirect) {
        form.getItens().removeIf(i -> i == null
                || ((i.getProdutoId() == null || i.getProdutoId().isBlank()) && i.getQuantidade() == null));
        try {
            Pedido pedido = pedidoService.registrar(form);
            redirect.addFlashAttribute("sucesso", "Pedido registrado. Total calculado: R$ " + pedido.getValorTotal());
            return "redirect:/pedidos";
        } catch (IllegalArgumentException | RegraNegocioException | NoSuchElementException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/pedidos/novo";
        }
    }
}
