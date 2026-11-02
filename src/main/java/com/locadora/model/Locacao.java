package com.locadora.model;

import com.locadora.util.ValidadorLocacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Locacao {

    private Long id;
    private Long clienteId;
    private Long filmeId;
    private int dias;
    private BigDecimal valorDiaria;
    private LocalDate dataLocacao;
    private LocalDate dataDevolucao;

    public Locacao() {}

    public Locacao(Long id, Long clienteId, Long filmeId, int dias, BigDecimal valorDiaria, LocalDate dataLocacao, LocalDate dataDevolucao) {
        this.id = id;
        this.clienteId = clienteId;
        this.filmeId = filmeId;
        this.dias = dias;
        this.valorDiaria = valorDiaria;
        this.dataLocacao = dataLocacao;
        this.dataDevolucao = dataDevolucao;

        ValidadorLocacao.validar(this);
    }

    public static Locacao nova(Long clienteId, Long filmeId, int dias,
                               BigDecimal valorDiaria) {

        Locacao locacao =  new Locacao(null, clienteId, filmeId, dias, valorDiaria, LocalDate.now(), null);
        ValidadorLocacao.validarNova(locacao);
        return locacao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = ValidadorLocacao.validarClienteId(clienteId);
    }

    public Long getFilmeId() {
        return filmeId;
    }

    public void setFilmeId(Long filmeId) {
        this.filmeId = ValidadorLocacao.validarFilmeId(filmeId);
    }

    public int getDias() {
        return dias;
    }

    public void setDias(int dias) {
        this.dias = ValidadorLocacao.validarDias(dias);
    }

    public BigDecimal getValorDiaria() {
        return valorDiaria;
    }

    public void setValorDiaria(BigDecimal valorDiaria) {
        this.valorDiaria = ValidadorLocacao.validarValorDiaria(valorDiaria);
    }

    public LocalDate getDataLocacao() {
        return dataLocacao;
    }

    public void setDataLocacao(LocalDate dataLocacao) {
        LocalDate dataValidada = ValidadorLocacao.validarDataLocacao(dataLocacao);
        if (dataDevolucao != null) {
            ValidadorLocacao.validarDatas(dataValidada, dataDevolucao);
        }
        this.dataLocacao = dataValidada;
    }

    public LocalDate getDataDevolucao() {
        return dataDevolucao;
    }

    public void setDataDevolucao(LocalDate dataDevolucao) {
        if (dataLocacao != null) {
            ValidadorLocacao.validarDatas(dataLocacao, dataDevolucao);
        }
        this.dataDevolucao = dataDevolucao;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Locacao locacao)) {
            return false;
        }
        return Objects.equals(id, locacao.id)
                && Objects.equals(clienteId, locacao.clienteId)
                && Objects.equals(filmeId, locacao.filmeId)
                && dias == locacao.dias
                && Objects.equals(valorDiaria, locacao.valorDiaria)
                && Objects.equals(dataLocacao, locacao.dataLocacao)
                && Objects.equals(dataDevolucao, locacao.dataDevolucao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, clienteId, filmeId, dias, valorDiaria, dataLocacao, dataDevolucao);
    }
}
