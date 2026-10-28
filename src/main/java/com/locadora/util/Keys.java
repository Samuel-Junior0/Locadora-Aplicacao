package com.locadora.util;

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
}
