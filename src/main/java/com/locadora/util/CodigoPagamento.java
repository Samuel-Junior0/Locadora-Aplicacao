package com.ifpb.hard_zone.util;

import jakarta.persistence.Embeddable;
import jdk.jfr.Name;

import java.util.UUID;

@Embeddable
public class CodigoPagamento {

    private String codigo;

    public CodigoPagamento(){}

    public CodigoPagamento(String codigo){
        this.codigo = ("PAG-HARDZONE-" + UUID.randomUUID()).toUpperCase();
    }

    public String getCodigoGerado(){
        return codigo;
    }
}
