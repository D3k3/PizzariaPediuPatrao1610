package com.umc.pediupatrao.web;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.CancelamentoPedidoService;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.DescontoPedidoService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.StatusPedidoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import com.umc.pediupatrao.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class, PedidoService.class, RotasEPermissoesTest.Relogio.class})
class RotasEPermissoesTest {

    @TestConfiguration
    static class Relogio {
        @Bean
        Clock relogio() {
            return Clock.systemDefaultZone();
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean private PedidoRepository pedidoRepository;
    @MockitoBean private ClienteRepository clienteRepository;
    @MockitoBean private ProdutoRepository produtoRepository;
    @MockitoBean private AuditoriaService auditoriaService;
    @MockitoBean private UsuarioDetailsService usuarioDetailsService;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private ClienteService clienteService;
    @MockitoBean private ProdutoService produtoService;
    @MockitoBean private StatusPedidoService statusPedidoService;
    @MockitoBean private CancelamentoPedidoService cancelamentoPedidoService;
    @MockitoBean private DescontoPedidoService descontoPedidoService;

    private ResultActions enviarJson(String url, String corpo) throws Exception {
        return mvc.perform(post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    @Test
    @WithMockUser(username = "atendente", roles = "ATENDENTE")
    void atendenteCriaPedidoEValoresDoJsonSaoIgnorados() throws Exception {
        Produto pizza = new Produto();
        pizza.setId("pizza");
        pizza.setNome("Pizza Calabresa");
        pizza.setPreco(30.0);
        pizza.setAtivo(true);
        when(produtoRepository.findById("pizza")).thenReturn(Optional.of(pizza));
        when(clienteRepository.existsById("c1")).thenReturn(true);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        enviarJson("/api/pedidos", """
                {"clienteId": "c1", "tipoAtendimento": "ENTREGA",
                 "itens": [{"produtoId": "pizza", "quantidade": 2}],
                 "valorTotal": 1, "percentualDesconto": 50, "status": "FINALIZADO"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(60.00))
                .andExpect(jsonPath("$.status").value("RECEBIDO"))
                .andExpect(jsonPath("$.percentualDesconto").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoPodeCriarPedido() throws Exception {
        enviarJson("/api/pedidos", "{}").andExpect(status().isForbidden());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoPodeCancelarPedido() throws Exception {
        enviarJson("/api/pedidos/1/cancelar", "{\"justificativa\": \"Cliente desistiu\"}")
                .andExpect(status().isForbidden());
        verify(cancelamentoPedidoService, never()).cancelar(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteNaoPodeDarDescontoDe25PorCento() throws Exception {
        enviarJson("/api/pedidos/1/desconto", "{\"percentual\": 25}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O desconto não pode passar de 20%."));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoPodeConsultarAuditoria() throws Exception {
        mvc.perform(get("/api/auditoria")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteNaoPodeAbrirTelaDeUsuarios() throws Exception {
        mvc.perform(get("/usuarios")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteNaoPodeCriarUsuarioAdminPelaApi() throws Exception {
        enviarJson("/api/usuarios", "{\"username\": \"novo\", \"password\": \"senha1234\", \"perfil\": \"ADMIN\"}")
                .andExpect(status().isForbidden());
        verify(usuarioService, never()).criar(any());
    }
}
