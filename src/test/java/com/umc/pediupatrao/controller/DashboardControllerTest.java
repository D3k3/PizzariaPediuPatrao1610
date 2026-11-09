package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.dto.dashboard.Ordem;
import com.umc.pediupatrao.dto.dashboard.OrdenarPor;
import com.umc.pediupatrao.dto.dashboard.PeriodoDto;
import com.umc.pediupatrao.dto.dashboard.ProdutoRankingDto;
import com.umc.pediupatrao.dto.dashboard.ResumoDashboardResponse;
import com.umc.pediupatrao.exception.ApiExceptionHandler;
import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class DashboardControllerTest {

    @Autowired private MockMvc mvc;

    @MockitoBean private DashboardService service;
    @MockitoBean private UsuarioDetailsService usuarioDetailsService;

    private ResumoDashboardResponse resumoExemplo() {
        return new ResumoDashboardResponse(
                new PeriodoDto(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 11, 8)),
                new BigDecimal("18450.90"), 87, new BigDecimal("212.08"), 9, new BigDecimal("640.10"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_recebe200EJsonDoResumo() throws Exception {
        when(service.resumo(any(), any(), any())).thenReturn(resumoExemplo());

        mvc.perform(get("/api/dashboard/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.inicio").value("2026-10-10"))
                .andExpect(jsonPath("$.periodo.fim").value("2026-11-08"))
                .andExpect(jsonPath("$.faturamento").value(18450.90))
                .andExpect(jsonPath("$.quantidadeVendas").value(87))
                .andExpect(jsonPath("$.ticketMedio").value(212.08))
                .andExpect(jsonPath("$.quantidadeCancelados").value(9))
                .andExpect(jsonPath("$.totalDescontos").value(640.10))
                .andExpect(jsonPath("$.clienteId").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerente_recebe200() throws Exception {
        when(service.resumo(any(), any(), any())).thenReturn(resumoExemplo());
        mvc.perform(get("/api/dashboard/resumo")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendente_recebe403ComErroApi() throws Exception {
        mvc.perform(get("/api/dashboard/resumo"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.erro").value("Acesso negado"))
                .andExpect(jsonPath("$.caminho").value("/api/dashboard/resumo"));
        verifyNoInteractions(service);
    }

    @Test
    void anonimo_recebe401ComErroApiEnaoRedirecionaParaLogin() throws Exception {
        mvc.perform(get("/api/dashboard/resumo"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.erro").value("Não autenticado"))
                .andExpect(jsonPath("$.caminho").value("/api/dashboard/resumo"));
        verifyNoInteractions(service);
    }

    @Test
    void anonimo_recebe401EmTodosOsEndpoints() throws Exception {
        for (String url : List.of("/api/dashboard/vendas-por-dia", "/api/dashboard/pedidos-por-status",
                "/api/dashboard/produtos", "/api/dashboard/picos/horarios", "/api/dashboard/picos/dias-semana",
                "/api/dashboard/ultimos-pedidos")) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendente_recebe403EmTodosOsEndpoints() throws Exception {
        for (String url : List.of("/api/dashboard/vendas-por-dia", "/api/dashboard/pedidos-por-status",
                "/api/dashboard/produtos", "/api/dashboard/picos/horarios", "/api/dashboard/picos/dias-semana",
                "/api/dashboard/ultimos-pedidos")) {
            mvc.perform(get(url)).andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void inicioMaiorQueFim_retorna400ComErroApi() throws Exception {
        when(service.resumo(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("A data inicial não pode ser posterior à data final."));

        mvc.perform(get("/api/dashboard/resumo?inicio=2026-11-05&fim=2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("A data inicial não pode ser posterior à data final."))
                .andExpect(jsonPath("$.caminho").value("/api/dashboard/resumo"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void limite50_retorna400ComErroApi() throws Exception {
        mvc.perform(get("/api/dashboard/produtos?limite=50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("limite deve estar entre 1 e 20."))
                .andExpect(jsonPath("$.caminho").value("/api/dashboard/produtos"));
        verify(service, never()).produtos(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void limite0_retorna400() throws Exception {
        mvc.perform(get("/api/dashboard/produtos?limite=0")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ordenarPorInvalido_retorna400() throws Exception {
        mvc.perform(get("/api/dashboard/produtos?ordenarPor=XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Valor inválido para o parâmetro 'ordenarPor'."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ordemInvalida_retorna400() throws Exception {
        mvc.perform(get("/api/dashboard/produtos?ordem=XYZ")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void tipoAtendimentoInvalido_retorna400() throws Exception {
        mvc.perform(get("/api/dashboard/resumo?tipoAtendimento=VOANDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Valor inválido para o parâmetro 'tipoAtendimento'."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dataMalFormatada_retorna400() throws Exception {
        mvc.perform(get("/api/dashboard/resumo?inicio=08/11/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Valor inválido para o parâmetro 'inicio'."));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void produtos_formatoDoJsonEPadroes() throws Exception {
        when(service.produtos(any(), any(), any(), eq(OrdenarPor.RECEITA), eq(Ordem.MAIOR), eq(10)))
                .thenReturn(List.of(new ProdutoRankingDto("p1", "Pizza Calabresa", 41,
                        new BigDecimal("2350.75"), new BigDecimal("12.74"))));

        mvc.perform(get("/api/dashboard/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].produtoId").value("p1"))
                .andExpect(jsonPath("$[0].nome").value("Pizza Calabresa"))
                .andExpect(jsonPath("$[0].quantidadeVendida").value(41))
                .andExpect(jsonPath("$[0].receita").value(2350.75))
                .andExpect(jsonPath("$[0].percentualDoFaturamento").value(12.74));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void produtos_repassaParametros() throws Exception {
        when(service.produtos(any(), any(), any(), any(), any(), anyInt())).thenReturn(List.of());

        mvc.perform(get("/api/dashboard/produtos?ordenarPor=QUANTIDADE&ordem=MENOR&limite=5&tipoAtendimento=RETIRADA"
                + "&inicio=2026-10-01&fim=2026-10-31"))
                .andExpect(status().isOk());

        verify(service).produtos(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                com.umc.pediupatrao.entity.TipoAtendimento.RETIRADA, OrdenarPor.QUANTIDADE, Ordem.MENOR, 5);
    }
}
