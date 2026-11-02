package com.locadora.model;

import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Year;
import java.util.Objects;

public class Filme {

    private Long id;
    private String titulo;
    private String genero;
    private Year ano;
    private Duration duracao;
    @Min(value = 0, message = "O valor não pode ser menor que zero.")
    private BigDecimal valorDiario;


    public Filme() {
    }
    public Filme(Long id, String titulo, String genero, Year ano, Duration duracao, BigDecimal valorDiario) {
        this.id = id;
        setTitulo(titulo);
        setGenero(genero);
        setAno(ano);
        setDuracao(duracao);
        setValorDiario(valorDiario);
    }

    public static Filme novo(String titulo, String genero, Year ano, Duration duracao, BigDecimal valorDiario){
        return new Filme(null, titulo, genero, ano, duracao, valorDiario);
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getValorDiario() {
        return valorDiario;
    }

    public Duration getDuracao() {
        return duracao;
    }

    public Year getAno() {
        return ano;
    }

    public String getGenero() {
        return genero;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public void setDuracao(Duration duracao) {
        this.duracao = duracao;
    }

    public void setValorDiario(BigDecimal valorDiario) {
        this.valorDiario = valorDiario;
    }

    public void setAno(Year ano) {
        this.ano = ano;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, titulo, genero, duracao, valorDiario);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        Filme filme = (Filme) object;
        return Objects.equals(id, filme.id) && Objects.equals(titulo, filme.titulo) && Objects.equals(genero, filme.genero) && Objects.equals(ano, filme.ano) && Objects.equals(duracao, filme.duracao) && Objects.equals(valorDiario, filme.valorDiario);
    }

    @Override
    public String toString() {
        return "Filme{" +
                "id=" + id +
                ", titulo='" + titulo + '\'' +
                ", genero='" + genero + '\'' +
                ", ano=" + ano +
                ", duracao=" + duracao +
                ", valorDiario=" + valorDiario +
                '}';
    }
}
