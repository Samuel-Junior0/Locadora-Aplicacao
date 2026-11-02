package com.locadora.repository;

import com.locadora.exception.ClienteNaoEncontradoException;
import com.locadora.exception.ClienteSemIdException;
import com.locadora.exception.CpfJaCadastradoException;
import com.locadora.exception.EntidadeNaoEncontradaException;
import com.locadora.model.Cliente;
import com.locadora.util.Keys;
import com.locadora.util.ValidadorCliente;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.Optional;

public final class ClienteRepository extends BaseRepository<Cliente> {

    public ClienteRepository(Jedis jedis) {
        super(jedis, Cliente.class, Keys::cliente, Keys.clientesIds(), Keys.seqCliente());
    }

    public Cliente inserir(String nome, String cpf) {
        return inserir(Cliente.novo(nome, cpf));
    }

    public Cliente atualizar(Cliente alterado) {
        if (alterado.getId() == null) {
            throw new ClienteSemIdException();
        }
        return atualizar(alterado.getId(), alterado);
    }

    public Optional<Cliente> buscarPorCpf(String cpf) {
        String id = jedis.get(Keys.idxClienteCpf(ValidadorCliente.validarCpfCliente(cpf)));
        if (id == null) {
            return Optional.empty();
        }
        return buscarPorId(Long.parseLong(id));
    }

    @Override
    protected void definirId(Cliente cliente, long id) {
        cliente.setId(id);
    }

    @Override
    protected EntidadeNaoEncontradaException naoEncontrada(long id) {
        return new ClienteNaoEncontradoException(id);
    }

    @Override
    protected String[] chavesIndices(Cliente cliente) {
        return new String[]{Keys.idxClienteCpf(cliente.getCpf())};
    }

    @Override
    protected void validarUnicidade(Cliente nova, Cliente atual) {
        String dono = jedis.get(Keys.idxClienteCpf(nova.getCpf()));
        boolean ocupadoPorOutro = dono != null
                && (atual == null || !dono.equals(String.valueOf(atual.getId())));
        if (ocupadoPorOutro) {
            throw new CpfJaCadastradoException(nova.getCpf());
        }
    }

    @Override
    protected void gravarIndices(Transaction tx, Cliente nova, Cliente atual) {
        tx.set(Keys.idxClienteCpf(nova.getCpf()), String.valueOf(nova.getId()));
        if (atual != null && !Keys.idxClienteCpf(atual.getCpf())
                .equals(Keys.idxClienteCpf(nova.getCpf()))) {
            tx.del(Keys.idxClienteCpf(atual.getCpf()));
        }
    }

    @Override
    protected void apagarIndices(Transaction tx, Cliente atual) {
        tx.del(Keys.idxClienteCpf(atual.getCpf()));
    }
}
