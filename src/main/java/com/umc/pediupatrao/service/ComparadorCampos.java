package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoCampo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ComparadorCampos {
    private static final List<String> TRECHOS_SENSIVEIS = List.of("password", "senha", "token", "hash", "secret");

    private ComparadorCampos() {
    }

    public static List<AlteracaoCampo> comparar(Object antes, Object depois) {
        if (antes == null && depois == null) {
            return List.of();
        }
        Class<?> tipo = antes != null ? antes.getClass() : depois.getClass();
        if (antes != null && depois != null && antes.getClass() != depois.getClass()) {
            throw new IllegalArgumentException("Só é possível comparar objetos da mesma classe.");
        }

        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        for (Field campo : camposComparaveis(tipo)) {
            String anterior = formatar(ler(campo, antes));
            String novo = formatar(ler(campo, depois));
            if (!Objects.equals(anterior, novo)) {
                alteracoes.add(new AlteracaoCampo(campo.getName(), anterior, novo));
            }
        }
        return alteracoes;
    }

    public static boolean isSensivel(String nomeCampo) {
        String nome = nomeCampo.toLowerCase(Locale.ROOT);
        return TRECHOS_SENSIVEIS.stream().anyMatch(nome::contains);
    }

    public static String formatar(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof BigDecimal numero) {
            return numero.toPlainString();
        }
        if (valor instanceof Enum<?> constante) {
            return constante.name();
        }
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private static List<Field> camposComparaveis(Class<?> tipo) {
        List<Field> campos = new ArrayList<>();
        for (Class<?> c = tipo; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field campo : c.getDeclaredFields()) {
                int mod = campo.getModifiers();
                if (Modifier.isStatic(mod) || campo.isSynthetic()
                        || "id".equals(campo.getName()) || isSensivel(campo.getName())) {
                    continue;
                }
                campo.setAccessible(true);
                campos.add(campo);
            }
        }
        return campos;
    }

    private static Object ler(Field campo, Object alvo) {
        if (alvo == null) {
            return null;
        }
        try {
            return campo.get(alvo);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Não foi possível ler o campo " + campo.getName(), e);
        }
    }
}
