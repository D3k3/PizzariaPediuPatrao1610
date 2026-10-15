package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.FiltroAuditoria;
import com.umc.pediupatrao.entity.EntidadeAuditada;
import com.umc.pediupatrao.entity.OperacaoAuditoria;
import com.umc.pediupatrao.service.AuditoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuditoriaTelaController {
    private final AuditoriaService auditoriaService;

    public AuditoriaTelaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/auditoria")
    public String consultar(@ModelAttribute("filtro") FiltroAuditoria filtro,
            @RequestParam(defaultValue = "0") int pagina, Model model) {
        try {
            model.addAttribute("resultado", auditoriaService.consultar(filtro, pagina, AuditoriaService.TAMANHO_PAGINA_PADRAO));
        } catch (IllegalArgumentException e) {
            model.addAttribute("erro", e.getMessage());
        }
        model.addAttribute("operacoes", OperacaoAuditoria.values());
        model.addAttribute("entidades", EntidadeAuditada.values());
        model.addAttribute("content", "auditoria/lista :: content");
        return "layout";
    }
}
