package com.locadora.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.locadora.util.ValidadorLocacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Locacao {

    private Long id;
    private Long clienteId;
    private Long filmeId;
    private BigDecimal valorDiaria;
    private LocalDate dataLocacao;
    private LocalDate dataDevolucao;

    public Locacao() {}

    public Locacao(Long id, Long clienteId, Long filmeId, BigDecimal valorDiaria,
                   LocalDate dataLocacao, LocalDate dataDevolucao) {
        this.id = id;
        this.clienteId = clienteId;
        this.filmeId = filmeId;
        this.valorDiaria = valorDiaria;
        this.dataLocacao = dataLocacao;
        this.dataDevolucao = dataDevolucao;

        ValidadorLocacao.validar(this);
    }

    public static Locacao nova(Long clienteId, Long filmeId, BigDecimal valorDiaria) {
        Locacao locacao = new Locacao(null, clienteId, filmeId, valorDiaria, LocalDate.now(), null);
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

    public Long periodoEmDias() {
        if (dataDevolucao == null) {
            return null;
        }
        return Math.max(1, ChronoUnit.DAYS.between(dataLocacao, dataDevolucao));
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
                && Objects.equals(valorDiaria, locacao.valorDiaria)
                && Objects.equals(dataLocacao, locacao.dataLocacao)
                && Objects.equals(dataDevolucao, locacao.dataDevolucao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, clienteId, filmeId, valorDiaria, dataLocacao, dataDevolucao);
    }
}
