package com.locadora.model;

import com.locadora.util.Cpf;

import java.util.Objects;

public class Cliente {

    private Long id;
    private String nome;
    private String cpf;

    public Cliente() {
    }

    public Cliente(Long id, String nome, String cpf) {
        this.id = id;
        setNome(nome);
        setCpf(cpf);
    }

    public static Cliente novo(String nome, String cpf) {
        return new Cliente(null, nome, cpf);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do cliente é obrigatório");
        }
        this.nome = nome.trim();
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = Cpf.normalizar(cpf);
    }

    @Override
    public String toString() {
        return "Cliente{id=" + id + ", nome='" + nome + "', cpf='" + cpf + "'}";
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Cliente cliente)) {
            return false;
        }
        return Objects.equals(id, cliente.id)
                && Objects.equals(nome, cliente.nome)
                && Objects.equals(cpf, cliente.cpf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nome, cpf);
    }
}
