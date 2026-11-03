package com.locadora.model;

import com.locadora.util.CodigoPagamento;

import javax.xml.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Pagamento {

    private Long id;
    private Long locacaoId;
    private BigDecimal valor;
    private LocalDate dataPagamento;
    private CodigoPagamento codigoPagamento;

    public Pagamento() {
    }


    public Pagamento(Long id, Long locacaoId, BigDecimal valor, LocalDate dataPagamento, CodigoPagamento codigoPagamento) {
        this.id = id;
        this.locacaoId = locacaoId;
        this.valor = valor;
        this.dataPagamento = dataPagamento;
        this.codigoPagamento = codigoPagamento;
        //Validar
    }

    public Pagamento novo(Long locacaoId, BigDecimal valor, LocalDate dataPagamento, CodigoPagamento codigoPagamento){

        Pagamento pagamento = new Pagamento(null, locacaoId, valor, dataPagamento, codigoPagamento);
        //Validar
        return pagamento;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLocacaoId() {
        return locacaoId;
    }

    public void setLocacaoId(Long locacaoId) {
        this.locacaoId = locacaoId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public CodigoPagamento getCodigoPagamento() {
        return codigoPagamento;
    }

    public void setCodigoPagamento(CodigoPagamento codigoPagamento) {
        this.codigoPagamento = codigoPagamento;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        Pagamento pagamento = (Pagamento) object;
        return Objects.equals(id, pagamento.id) && Objects.equals(locacaoId, pagamento.locacaoId) && Objects.equals(valor, pagamento.valor) && Objects.equals(dataPagamento, pagamento.dataPagamento) && Objects.equals(codigoPagamento, pagamento.codigoPagamento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, locacaoId, valor, dataPagamento, codigoPagamento);
    }
}
