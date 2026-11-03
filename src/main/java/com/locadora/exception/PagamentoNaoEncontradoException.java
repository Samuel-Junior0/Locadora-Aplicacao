package com.locadora.exception;

public class PagamentoNaoEncontradoException extends RuntimeException {
  public PagamentoNaoEncontradoException(String message) {
    super(message);
  }
}
