package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.CancelamentoRequest;
import com.umc.pediupatrao.dto.DescontoRequest;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.CancelamentoPedidoService;
import com.umc.pediupatrao.service.DescontoPedidoService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.StatusPedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final StatusPedidoService statusPedidoService;
    private final CancelamentoPedidoService cancelamentoPedidoService;
    private final DescontoPedidoService descontoPedidoService;

    public PedidoController(PedidoService pedidoService, StatusPedidoService statusPedidoService,
            CancelamentoPedidoService cancelamentoPedidoService, DescontoPedidoService descontoPedidoService) {
        this.pedidoService = pedidoService;
        this.statusPedidoService = statusPedidoService;
        this.cancelamentoPedidoService = cancelamentoPedidoService;
        this.descontoPedidoService = descontoPedidoService;
    }

    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @GetMapping("/{id}")
    public Pedido buscar(@PathVariable String id) {
        return pedidoService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Pedido> registrar(@Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.registrar(request));
    }

    @PutMapping("/{id}/status")
    public Pedido avancarStatus(@PathVariable String id, @RequestParam StatusPedido status) {
        return statusPedidoService.avancar(id, status);
    }

    @PostMapping("/{id}/cancelar")
    public Pedido cancelar(@PathVariable String id, @Valid @RequestBody CancelamentoRequest request) {
        return cancelamentoPedidoService.cancelar(id, request.getJustificativa());
    }

    @PostMapping("/{id}/desconto")
    public Pedido aplicarDesconto(@PathVariable String id, @Valid @RequestBody DescontoRequest request) {
        return descontoPedidoService.aplicar(id, request.getPercentual());
    }
}
