package com.locadora.util;

import jdk.jshell.execution.Util;

import java.util.Locale;

public final class Keys {

    private Keys() {
    }
    public static String cliente(long id) {
        return "cliente:" + id;
    }

    public static String seqCliente() {
        return "seq:cliente";
    }

    public static String idxClienteCpf(String cpf) {
        return "idx:cliente:cpf:" + cpf;
    }

    public static String clientesIds() {
        return "clientes:ids";
    }

    public static String filme(long id) {
        return "filme:" + id;
    }

    public static String seqFilme() {return "seq:filme";}

    public static String filmesIds() {
        return "filmes:ids";
    }


    public static String idxFilmeTitulo(String titulo) {
        return "idx:filme:titulo:" + titulo.trim().toLowerCase(Locale.ROOT);
    }
}
