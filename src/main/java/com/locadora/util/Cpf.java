package com.locadora.util;

public final class Cpf {

    private Cpf() {
    }

    public static String normalizar(String cpf) {
        if (cpf == null) {
            throw new IllegalArgumentException("CPF é obrigatório");
        }
        String digitos = cpf.replaceAll("\\D", "");
        if (digitos.length() != 11) {
            throw new IllegalArgumentException("CPF deve ter 11 dígitos: " + cpf);
        }
        return digitos;
    }

    public static String formatar(String cpf) {
        String d = normalizar(cpf);
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9)
                + "-" + d.substring(9);
    }
}
