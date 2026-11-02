package com.locadora.interfaces;

import java.util.List;
import java.util.Optional;

public interface Repository <T>{

    T inserir(T entidade);
    Optional<T> buscarPorId(long id);
    T atualizar(long id, T entidadeAtualizada);
    boolean remover(long id);
    List<T> listarTodos();
}
