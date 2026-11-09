package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.dashboard.Ordem;
import com.umc.pediupatrao.dto.dashboard.OrdenarPor;
import com.umc.pediupatrao.dto.dashboard.PicoDiaSemanaResponse;
import com.umc.pediupatrao.dto.dashboard.PicoHorarioResponse;
import com.umc.pediupatrao.dto.dashboard.ProdutoRankingDto;
import com.umc.pediupatrao.dto.dashboard.ResumoDashboardResponse;
import com.umc.pediupatrao.dto.dashboard.StatusQuantidadeDto;
import com.umc.pediupatrao.dto.dashboard.UltimoPedidoDto;
import com.umc.pediupatrao.dto.dashboard.VendaDiaDto;
import com.umc.pediupatrao.entity.TipoAtendimento;
import com.umc.pediupatrao.service.DashboardService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
@Validated
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/resumo")
    public ResumoDashboardResponse resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento) {
        return service.resumo(inicio, fim, tipoAtendimento);
    }

    @GetMapping("/vendas-por-dia")
    public List<VendaDiaDto> vendasPorDia(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento) {
        return service.vendasPorDia(inicio, fim, tipoAtendimento);
    }

    @GetMapping("/pedidos-por-status")
    public List<StatusQuantidadeDto> pedidosPorStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento) {
        return service.pedidosPorStatus(inicio, fim, tipoAtendimento);
    }

    @GetMapping("/produtos")
    public List<ProdutoRankingDto> produtos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento,
            @RequestParam(defaultValue = "RECEITA") OrdenarPor ordenarPor,
            @RequestParam(defaultValue = "MAIOR") Ordem ordem,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "limite deve estar entre 1 e 20.")
            @Max(value = 20, message = "limite deve estar entre 1 e 20.") int limite) {
        return service.produtos(inicio, fim, tipoAtendimento, ordenarPor, ordem, limite);
    }

    @GetMapping("/picos/horarios")
    public PicoHorarioResponse picosHorarios(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento) {
        return service.picosHorarios(inicio, fim, tipoAtendimento);
    }

    @GetMapping("/picos/dias-semana")
    public PicoDiaSemanaResponse picosDiasSemana(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento) {
        return service.picosDiasSemana(inicio, fim, tipoAtendimento);
    }

    @GetMapping("/ultimos-pedidos")
    public List<UltimoPedidoDto> ultimosPedidos(@RequestParam(required = false) TipoAtendimento tipoAtendimento) {
        return service.ultimosPedidos(tipoAtendimento);
    }
}
