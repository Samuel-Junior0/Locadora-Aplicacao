package com.locadora.exception;

public class PagamentoNaoEncontradoException extends EntidadeNaoEncontradaException {
    public PagamentoNaoEncontradoException(long id) {
        super("Pagamento não encontrado: " + id);
    }
}
