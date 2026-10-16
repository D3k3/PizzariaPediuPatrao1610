package com.umc.pediupatrao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CancelamentoRequest {

    public static final int JUSTIFICATIVA_MAXIMA = 500;

    @NotBlank(message = "A justificativa do cancelamento é obrigatória.")
    @Size(max = JUSTIFICATIVA_MAXIMA, message = "A justificativa deve ter no máximo 500 caracteres.")
    private String justificativa;

    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
}
