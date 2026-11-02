package com.locadora.service;

import com.locadora.interfaces.Service;
import com.locadora.repository.BaseRepository;

import java.util.List;
import java.util.Optional;

public class BaseService <T> implements Service<T> {

    protected final BaseRepository<T> repository;

    protected BaseService(BaseRepository<T> repository) {
        this.repository = repository;
    }

    @Override
    public Optional<T> buscarPorId(long id) {
        return repository.buscarPorId(id);
    }

    @Override
    public List<T> listarTodos() {
        return repository.listarTodos();
    }

    @Override
    public T atualizar(long id, T entidadeAtualizada) {
        return repository.atualizar(id, entidadeAtualizada);
    }

    @Override
    public boolean remover(long id) {
       return repository.remover(id);
    }

}
