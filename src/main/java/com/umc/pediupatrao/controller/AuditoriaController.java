package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.AuditoriaEventoResponse;
import com.umc.pediupatrao.dto.FiltroAuditoria;
import com.umc.pediupatrao.dto.PaginaResponse;
import com.umc.pediupatrao.service.AuditoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {
    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public PaginaResponse<AuditoriaEventoResponse> consultar(@ModelAttribute FiltroAuditoria filtro,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "" + AuditoriaService.TAMANHO_PAGINA_PADRAO) int tamanho) {
        return auditoriaService.consultar(filtro, pagina, tamanho);
    }
}
