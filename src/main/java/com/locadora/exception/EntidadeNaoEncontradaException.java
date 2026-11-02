package com.locadora.exception;

public abstract class EntidadeNaoEncontradaException extends RuntimeException {

    protected EntidadeNaoEncontradaException(String message) {
        super(message);
    }
}
