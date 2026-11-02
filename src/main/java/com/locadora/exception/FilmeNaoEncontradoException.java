package com.locadora.exception;

public class FilmeNaoEncontradoException extends EntidadeNaoEncontradaException {
    public FilmeNaoEncontradoException(Long id) {
        super("Filme não encontrado: " + id);
    }
}
