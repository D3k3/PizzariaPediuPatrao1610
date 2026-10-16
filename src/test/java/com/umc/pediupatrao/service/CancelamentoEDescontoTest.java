package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.EntidadeAuditada;
import com.umc.pediupatrao.entity.OperacaoAuditoria;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelamentoEDescontoTest {

    private PedidoRepository repo;
    private AuditoriaService auditoria;

    @BeforeEach
    void preparar() {
        repo = mock(PedidoRepository.class);
        auditoria = mock(AuditoriaService.class);
        when(repo.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "gerente", "x", List.of(new SimpleGrantedAuthority("ROLE_GERENTE"))));
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private void pedidoNoBanco(StatusPedido status) {
        Pedido pedido = new Pedido();
        pedido.setId("1");
        pedido.setStatus(status);
        pedido.setTipoAtendimento(TipoAtendimento.ENTREGA);
        pedido.setValorBruto(new BigDecimal("100.00"));
        pedido.setValorTotal(new BigDecimal("100.00"));
        when(repo.findById("1")).thenReturn(Optional.of(pedido));
    }

    @Test
    void gerenteCancelaComJustificativaEGeraAuditoria() {
        pedidoNoBanco(StatusPedido.EM_PREPARACAO);
        CancelamentoPedidoService service = new CancelamentoPedidoService(repo, auditoria, Clock.systemDefaultZone());

        Pedido salvo = service.cancelar("1", "Cliente desistiu");

        assertEquals(StatusPedido.CANCELADO, salvo.getStatus());
        assertEquals("gerente", salvo.getCanceladoPor());
        verify(auditoria).registrar(eq(OperacaoAuditoria.CANCELAMENTO), eq(EntidadeAuditada.PEDIDO), eq("1"),
                anyList(), eq("Cliente desistiu"));
    }

    @Test
    void naoPodeDarDescontoEmPedidoQueJaSaiu() {
        pedidoNoBanco(StatusPedido.SAIU_PARA_ENTREGA);
        DescontoPedidoService service = new DescontoPedidoService(repo, auditoria);

        assertThrows(RegraNegocioException.class, () -> service.aplicar("1", new BigDecimal("10")));
        verify(repo, never()).save(any());
    }
}
