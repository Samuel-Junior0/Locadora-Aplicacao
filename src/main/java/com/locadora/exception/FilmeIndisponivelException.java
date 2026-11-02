package com.locadora.exception;

public class FilmeIndisponivelException extends RuntimeException {

    public FilmeIndisponivelException(long filmeId) {
        super("Filme indisponível: " + filmeId);
    }
}
