package com.locadora.interfaces;

import java.util.List;
import java.util.Optional;

public interface Service <T>{

    Optional<T> buscarPorId(long id);
    List<T> listarTodos();
    boolean remover(long id);
    T atualizar(long id, T entidadeAtualizada);

}
