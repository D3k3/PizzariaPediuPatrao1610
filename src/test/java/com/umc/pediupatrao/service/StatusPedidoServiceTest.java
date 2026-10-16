package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoAtendimento;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.exception.TransicaoStatusInvalidaException;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatusPedidoServiceTest {

    private final Clock relogio = Clock.fixed(Instant.parse("2026-10-16T20:00:00Z"), ZoneId.of("UTC"));
    private PedidoRepository repo;
    private StatusPedidoService service;

    @BeforeEach
    void preparar() {
        repo = mock(PedidoRepository.class);
        when(repo.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
        service = new StatusPedidoService(repo, mock(AuditoriaService.class), relogio);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "gerente", "x", List.of(new SimpleGrantedAuthority("ROLE_GERENTE"))));
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private Pedido pedidoNoBanco(StatusPedido status) {
        Pedido pedido = new Pedido();
        pedido.setId("1");
        pedido.setStatus(status);
        pedido.setTipoAtendimento(TipoAtendimento.ENTREGA);
        when(repo.findById("1")).thenReturn(Optional.of(pedido));
        return pedido;
    }

    @Test
    void avancaDeRecebidoParaEmPreparacao() {
        pedidoNoBanco(StatusPedido.RECEBIDO);

        Pedido salvo = service.avancar("1", StatusPedido.EM_PREPARACAO);

        assertEquals(StatusPedido.EM_PREPARACAO, salvo.getStatus());
        assertEquals(1, salvo.getHistoricoStatus().size());
        assertEquals(StatusPedido.EM_PREPARACAO, salvo.getHistoricoStatus().get(0).getStatus());
    }

    @Test
    void naoPodePularDeProntoParaFinalizado() {
        pedidoNoBanco(StatusPedido.PRONTO);

        assertThrows(TransicaoStatusInvalidaException.class, () -> service.avancar("1", StatusPedido.FINALIZADO));
        verify(repo, never()).save(any());
    }

    @Test
    void saidaParaEntregaGravaDataEResponsavel() {
        pedidoNoBanco(StatusPedido.PRONTO);

        Pedido salvo = service.avancar("1", StatusPedido.SAIU_PARA_ENTREGA);

        assertEquals(LocalDateTime.now(relogio), salvo.getDataSaida());
        assertEquals("gerente", salvo.getResponsavelSaida());
    }

    @Test
    void pedidoCanceladoNaoMudaDeStatus() {
        pedidoNoBanco(StatusPedido.CANCELADO);

        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.avancar("1", StatusPedido.EM_PREPARACAO));
        assertEquals("Pedido cancelado não pode ser alterado.", erro.getMessage());
    }
}
