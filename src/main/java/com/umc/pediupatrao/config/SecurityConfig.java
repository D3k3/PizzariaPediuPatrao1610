package com.umc.pediupatrao.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.pediupatrao.exception.ErroApi;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfException;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Desativa CSRF para REST APIs
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/login", "/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**", "/error").permitAll()

                        .requestMatchers("/usuarios/**", "/api/usuarios/**").hasRole("ADMIN")

                        .requestMatchers("/auditoria/**", "/api/auditoria/**").hasAnyRole("ADMIN", "GERENTE")

                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/clientes/**").hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/clientes/**").hasRole("ATENDENTE")
                        .requestMatchers("/clientes/novo", "/clientes/salvar", "/clientes/editar/**").hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.GET, "/clientes", "/api/clientes/**").hasAnyRole("GERENTE", "ATENDENTE")

                        .requestMatchers(HttpMethod.PUT, "/api/pedidos/*/status").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/pedidos/*/cancelar", "/api/pedidos/*/desconto").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers("/pedidos/novo", "/pedidos/salvar").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.GET, "/pedidos", "/api/pedidos/**").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")

                        .requestMatchers("/produtos/novo", "/produtos/salvar", "/produtos/editar/**", "/produtos/excluir/**")
                                .hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.GET, "/produtos", "/api/produtos/**").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.POST, "/api/produtos/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/produtos/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/produtos/**").hasAnyRole("ADMIN", "GERENTE")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint())
                        .accessDeniedHandler(acessoNegadoHandler())
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(authenticationSuccessHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            response.sendRedirect("/");
        };
    }

    private AuthenticationEntryPoint entryPoint() {
        LoginUrlAuthenticationEntryPoint paraLogin = new LoginUrlAuthenticationEntryPoint("/login");
        return (request, response, authException) -> {
            registrar("Não autenticado", request);
            if (ehApi(request)) {
                escreverJson(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Não autenticado");
            } else {
                paraLogin.commence(request, response, authException);
            }
        };
    }

    private AccessDeniedHandler acessoNegadoHandler() {
        return (request, response, accessDeniedException) -> {
            String motivo = accessDeniedException instanceof CsrfException
                    ? "Token CSRF ausente ou inválido" : "Acesso negado";
            registrar(motivo, request);
            if (ehApi(request)) {
                escreverJson(request, response, HttpServletResponse.SC_FORBIDDEN, motivo);
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        };
    }

    private boolean ehApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    private void escreverJson(HttpServletRequest request, HttpServletResponse response,
            int status, String erro) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        JSON.writeValue(response.getWriter(), ErroApi.corpo(status, erro, request.getRequestURI()));
    }

    private void registrar(String motivo, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = (auth == null || auth instanceof AnonymousAuthenticationToken)
                ? "anônimo" : auth.getName();
        log.warn("{}: usuário={} {} {}", motivo, usuario, request.getMethod(), request.getRequestURI());
    }
}
