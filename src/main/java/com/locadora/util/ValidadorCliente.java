package com.locadora.util;

import com.locadora.exception.CampoObrigatorioException;

public final class ValidadorCliente {

    private ValidadorCliente() {
    }

    public static String validarNomeCliente(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new CampoObrigatorioException("Nome do cliente");
        }
        return nome.trim();
    }

    public static String validarCpfCliente(String cpf) {
        return Cpf.normalizar(cpf);
    }
}
