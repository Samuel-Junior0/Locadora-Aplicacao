package com.locadora.repository;

import com.locadora.exception.ClienteNaoEncontradoException;
import com.locadora.exception.CpfJaCadastradoException;
import com.locadora.model.Cliente;
import com.locadora.util.Cpf;
import com.locadora.util.Json;
import com.locadora.util.Keys;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ClienteRepository {

    private final Jedis jedis;

    public ClienteRepository(Jedis jedis) {
        this.jedis = jedis;
    }

    public Cliente inserir(String nome, String cpf) {
        Cliente novo = Cliente.novo(nome, cpf);
        String chaveCpf = Keys.idxClienteCpf(novo.getCpf());

        while (true) {
            jedis.watch(chaveCpf);
            if (jedis.exists(chaveCpf)) {
                jedis.unwatch();
                throw new CpfJaCadastradoException(novo.getCpf());
            }

            long id = jedis.incr(Keys.seqCliente());
            novo.setId(id);
            String chaveCliente = Keys.cliente(id);
            String json = Json.paraJson(novo);
            String idTexto = String.valueOf(id);

            Transaction tx = jedis.multi();
            tx.set(chaveCliente, json);
            tx.set(chaveCpf, idTexto);
            tx.sadd(Keys.clientesIds(), idTexto);

            if (tx.exec() != null) {
                return novo;
            }
        }
    }

    public Cliente atualizar(Cliente alterado) {
        if (alterado.getId() == null) {
            throw new IllegalArgumentException("Cliente sem id; use inserir() para um novo registro");
        }

        String cpfNovo = alterado.getCpf();
        String chaveCliente = Keys.cliente(alterado.getId());
        String chaveCpfNova = Keys.idxClienteCpf(cpfNovo);
        String json = Json.paraJson(alterado);
        String idTexto = String.valueOf(alterado.getId());

        while (true) {
            jedis.watch(chaveCliente, chaveCpfNova);
            String jsonAtual = jedis.get(chaveCliente);
            if (jsonAtual == null) {
                jedis.unwatch();
                throw new ClienteNaoEncontradoException(alterado.getId());
            }

            Cliente atual;
            try {
                atual = Json.paraObjeto(jsonAtual, Cliente.class);
            } catch (RuntimeException e) {
                jedis.unwatch();
                throw e;
            }
            String cpfAntigo = atual.getCpf();

            String dono = jedis.get(chaveCpfNova);
            if (dono != null && !dono.equals(idTexto)) {
                jedis.unwatch();
                throw new CpfJaCadastradoException(cpfNovo);
            }

            Transaction tx = jedis.multi();
            tx.set(chaveCliente, json);
            tx.set(chaveCpfNova, idTexto);
            if (!cpfAntigo.equals(cpfNovo)) {
                tx.del(Keys.idxClienteCpf(cpfAntigo));
            }

            if (tx.exec() != null) {
                return alterado;
            }
        }
    }

    public boolean remover(long id) {
        while (true) {
            jedis.watch(Keys.cliente(id));
            String json = jedis.get(Keys.cliente(id));
            if (json == null) {
                jedis.unwatch();
                return false;
            }

            Cliente cliente = Json.paraObjeto(json, Cliente.class);
            Transaction tx = jedis.multi();
            tx.del(Keys.cliente(id));
            tx.del(Keys.idxClienteCpf(cliente.getCpf()));
            tx.srem(Keys.clientesIds(), String.valueOf(id));

            if (tx.exec() != null) {
                return true;
            }
        }
    }

    public Optional<Cliente> buscarPorId(long id) {
        String json = jedis.get(Keys.cliente(id));
        return Optional.ofNullable(json)
                .map(valor -> Json.paraObjeto(valor, Cliente.class));
    }

    public Optional<Cliente> buscarPorCpf(String cpf) {
        String id = jedis.get(Keys.idxClienteCpf(Cpf.normalizar(cpf)));
        if (id == null) {
            return Optional.empty();
        }
        return buscarPorId(Long.parseLong(id));
    }

    public List<Cliente> listarTodos() {
        Set<String> ids = jedis.smembers(Keys.clientesIds());
        if (ids.isEmpty()) {
            return List.of();
        }

        List<Long> ordenados = ids.stream()
                .map(Long::parseLong)
                .sorted()
                .toList();
        String[] chaves = ordenados.stream()
                .map(id -> Keys.cliente(id))
                .toArray(String[]::new);
        List<String> jsons = jedis.mget(chaves);

        List<Cliente> clientes = new ArrayList<>();
        for (String json : jsons) {
            if (json != null) {
                clientes.add(Json.paraObjeto(json, Cliente.class));
            }
        }
        return clientes;
    }
}
