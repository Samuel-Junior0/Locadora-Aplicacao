package com.locadora.service;

import com.locadora.model.Cliente;
import com.locadora.repository.ClienteRepository;

import java.util.List;
import java.util.Optional;

public final class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    public Cliente cadastrar(String nome, String cpf) {
        return repository.inserir(nome, cpf);
    }

    public Cliente atualizar(long id, String nome, String cpf) {
        return repository.atualizar(new Cliente(id, nome, cpf));
    }

    public boolean remover(long id) {
        return repository.remover(id);
    }

    public Optional<Cliente> buscarPorId(long id) {
        return repository.buscarPorId(id);
    }

    public Optional<Cliente> buscarPorCpf(String cpf) {
        return repository.buscarPorCpf(cpf);
    }

    public List<Cliente> listar() {
        return repository.listarTodos();
    }
}
