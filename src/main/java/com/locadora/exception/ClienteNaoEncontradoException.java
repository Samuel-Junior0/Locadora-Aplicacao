package com.locadora.exception;

public class ClienteNaoEncontradoException extends EntidadeNaoEncontradaException {

    public ClienteNaoEncontradoException(long id) {
        super("Cliente não encontrado: " + id);
    }
}
