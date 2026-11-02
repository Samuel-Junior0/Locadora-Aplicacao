package com.locadora.exception;

public class LocacaoJaDevolvidaException extends LocacaoInvalidaException {

    public LocacaoJaDevolvidaException(long id) {
        super("Locação já devolvida: " + id);
    }
}
