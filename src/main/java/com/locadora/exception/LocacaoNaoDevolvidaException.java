package com.locadora.exception;

public class LocacaoNaoDevolvidaException extends RuntimeException {
    public LocacaoNaoDevolvidaException(long id) {
        super("Locação nãio devolvida: " + id);
    }
}
