package com.locadora.exception;

public class ClienteSemIdException extends ValidacaoException {

    public ClienteSemIdException() {
        super("Cliente sem id; use inserir() para um novo registro");
    }
}
