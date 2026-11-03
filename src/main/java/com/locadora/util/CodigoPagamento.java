package com.locadora.util;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class CodigoPagamento {

    private String codigo;

    public CodigoPagamento(){
        this.codigo = ("PAG-LOCADORA-" + UUID.randomUUID()).toUpperCase();
    }

    @JsonCreator
    public CodigoPagamento(@JsonProperty("codigoGerado") String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Código de pagamento é obrigatório");
        }
        this.codigo = codigo;
    }

    @JsonProperty("codigoGerado")
    public String getCodigoGerado(){
        return codigo;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        CodigoPagamento that = (CodigoPagamento) object;
        return Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(codigo);
    }
}
