package com.umc.pediupatrao.exception;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ErroApi {
    private ErroApi() {
    }

    public static Map<String, Object> corpo(int status, String erro, String caminho) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("status", status);
        corpo.put("erro", erro);
        corpo.put("caminho", caminho);
        return corpo;
    }
}
