package com.locadora.exception;

public class LocacaoNaoEncontradaException extends EntidadeNaoEncontradaException {

    public LocacaoNaoEncontradaException(long id) {
        super("Locação não encontrada: " + id);
    }
}
