package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record PicoHorarioResponse(Integer horaPico, List<Hora> itens) {
    public record Hora(int hora, long quantidade, BigDecimal faturamento) {
    }
}
