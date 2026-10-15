package com.umc.pediupatrao.service;

public final class MascaraDados {
    private static final int DIGITOS_VISIVEIS = 4;

    private MascaraDados() {
    }

    public static String telefone(String telefone) {
        if (telefone == null) {
            return null;
        }
        long totalDigitos = telefone.chars().filter(Character::isDigit).count();
        long aOcultar = Math.max(0, totalDigitos - DIGITOS_VISIVEIS);

        StringBuilder mascarado = new StringBuilder(telefone.length());
        for (char c : telefone.toCharArray()) {
            if (Character.isDigit(c) && aOcultar > 0) {
                mascarado.append('*');
                aOcultar--;
            } else {
                mascarado.append(c);
            }
        }
        return mascarado.toString();
    }
}
