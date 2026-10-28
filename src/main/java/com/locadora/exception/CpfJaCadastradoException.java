package com.locadora.exception;

public class CpfJaCadastradoException extends RuntimeException {

    public CpfJaCadastradoException(String cpf) {
        super("Já existe um cliente com o CPF " + cpf);
    }
}
