package com.locadora.exception;

public class ClienteNaoEncontradoException extends RuntimeException {

    public ClienteNaoEncontradoException(long id) {
        super("Cliente " + id + " não encontrado");
    }
}
