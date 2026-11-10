package com.umc.pediupatrao.web;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.controller.DashboardTelaController;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardTelaController.class)
@Import(SecurityConfig.class)
class DashboardTelaTest {

    @Autowired private MockMvc mvc;

    @MockitoBean private UsuarioDetailsService usuarioDetailsService;

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteAbreODashboard() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"dashboard\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/dashboard.js")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminAbreODashboard() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteRecebe403NoDashboard() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().isForbidden());
    }

    @Test
    void anonimoEhRedirecionadoParaLogin() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
