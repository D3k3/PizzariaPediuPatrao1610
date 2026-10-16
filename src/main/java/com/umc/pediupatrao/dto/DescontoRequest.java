package com.umc.pediupatrao.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class DescontoRequest {

    @NotNull(message = "Informe o percentual de desconto.")
    @DecimalMin(value = "0", inclusive = false, message = "O desconto deve ser maior que 0%.")
    @DecimalMax(value = "20", message = "O desconto não pode passar de 20%.")
    @Digits(integer = 3, fraction = 2, message = "Use no máximo 2 casas decimais no percentual.")
    private BigDecimal percentual;

    public BigDecimal getPercentual() { return percentual; }
    public void setPercentual(BigDecimal percentual) { this.percentual = percentual; }
}
