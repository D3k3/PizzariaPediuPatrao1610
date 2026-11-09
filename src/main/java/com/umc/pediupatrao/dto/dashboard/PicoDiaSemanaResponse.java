package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

public record PicoDiaSemanaResponse(DayOfWeek diaPico, List<Dia> itens) {
    public record Dia(DayOfWeek dia, String nome, long quantidade, BigDecimal faturamento) {
    }
}
