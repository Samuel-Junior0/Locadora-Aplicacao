package com.locadora.exception;

public class TituloJaExistenteException extends RuntimeException{

    public TituloJaExistenteException(String titulo){super("Titulo já existente: " + titulo);}
}
