package com.locadora.exception;

public class FilmeNaoEncontradoException extends EntidadeNaoEncontradaException {

    public FilmeNaoEncontradoException(long id) {
        super("Filme não encontrado: " + id);
    }
}
