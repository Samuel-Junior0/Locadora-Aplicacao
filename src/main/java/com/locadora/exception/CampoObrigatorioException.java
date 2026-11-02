package com.locadora.exception;

public class CampoObrigatorioException extends ValidacaoException {

    public CampoObrigatorioException(String campo) {
        super(campo + " é obrigatório");
    }
}
