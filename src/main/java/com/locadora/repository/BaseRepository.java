package com.locadora.repository;

import com.locadora.exception.EntidadeNaoEncontradaException;
import com.locadora.interfaces.Repository;
import com.locadora.model.Cliente;
import com.locadora.util.Json;
import com.locadora.util.Keys;
import org.slf4j.helpers.Util;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.*;
import java.util.function.Function;

public abstract class BaseRepository <T> implements Repository <T> {

    protected final Jedis jedis;
    protected final Class<T> tipo;
    protected final Function<Long, String> keyGenerator;
    protected final String setIdsKey;
    protected final String seqKey;

    protected BaseRepository(Jedis jedis, Class<T> tipo, Function<Long, String> keyGenerator,
                             String setIdsKey, String seqKey) {
        this.jedis = jedis;
        this.tipo = tipo;
        this.keyGenerator = keyGenerator;
        this.setIdsKey = setIdsKey;
        this.seqKey = seqKey;
    }
    protected abstract void definirId(T entidade, long id);

    protected abstract EntidadeNaoEncontradaException naoEncontrada(long id);

    protected String[] chavesIndices(T entidade) {
        return new String[0];
    }

    protected void validarUnicidade(T nova, T atual) {}

    protected void gravarIndices(Transaction tx, T nova, T atual) {}

    protected void apagarIndices(Transaction tx, T atual) {}


    public Optional<T> buscarPorId(long id) {
        String key = keyGenerator.apply(id);
        String json = jedis.get(key);
        return Optional.ofNullable(json)
                .map(valor -> Json.paraObjeto(valor, tipo));
    }

    @Override
    public T inserir(T nova) {
        Objects.requireNonNull(nova, "entidade é obrigatória");

        while (true) {
            observar(chavesIndices(nova));

            long id;
            String json;
            try {
                validarUnicidade(nova, null);
                id = jedis.incr(seqKey);
                definirId(nova, id);
                json = Json.paraJson(nova);
            } catch (RuntimeException e) {
                jedis.unwatch();
                throw e;
            }

            Transaction tx = jedis.multi();
            tx.set(keyGenerator.apply(id), json);
            tx.sadd(setIdsKey, String.valueOf(id));
            gravarIndices(tx, nova, null);

            if (tx.exec() != null) {
                return nova;
            }
        }
    }

    @Override
    public T atualizar(long id, T nova) {
        Objects.requireNonNull(nova, "entidade é obrigatória");
        definirId(nova, id);

        String key = keyGenerator.apply(id);
        String json = Json.paraJson(nova);

        String[] indices = chavesIndices(nova);
        String[] observadas = new String[indices.length + 1];
        observadas[0] = key;
        System.arraycopy(indices, 0, observadas, 1, indices.length);

        while (true) {
            observar(observadas);

            T atual;
            try {
                String jsonAtual = jedis.get(key);
                if (jsonAtual == null) {
                    throw naoEncontrada(id);
                }
                atual = Json.paraObjeto(jsonAtual, tipo);
                validarUnicidade(nova, atual);
            } catch (RuntimeException e) {
                jedis.unwatch();
                throw e;
            }

            Transaction tx = jedis.multi();
            tx.set(key, json);
            gravarIndices(tx, nova, atual);

            if (tx.exec() != null) {
                return nova;
            }
        }
    }

    @Override
    public boolean remover(long id) {
        String key = keyGenerator.apply(id);

        while (true) {
            jedis.watch(key);

            T atual;
            try {
                String json = jedis.get(key);
                if (json == null) {
                    jedis.unwatch();
                    return false;
                }
                atual = Json.paraObjeto(json, tipo);
            } catch (RuntimeException e) {
                jedis.unwatch();
                throw e;
            }

            Transaction tx = jedis.multi();
            tx.del(key);
            tx.srem(setIdsKey, String.valueOf(id));
            apagarIndices(tx, atual);

            if (tx.exec() != null) {
                return true;
            }
        }
    }

    @Override
    public List<T> listarTodos() {
        if (setIdsKey == null) {
            return List.of();
        }

        return listarPorIds(jedis.smembers(setIdsKey));
    }

    protected List<T> listarPorIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Long> ordenados = ids.stream()
                .map(Long::parseLong)
                .sorted()
                .toList();

        String[] chaves = ordenados.stream()
                .map(keyGenerator)
                .toArray(String[]::new);

        List<String> jsons = jedis.mget(chaves);

        List<T> entidades = new ArrayList<>();
        for (String json : jsons) {
            if (json != null) {
                entidades.add(Json.paraObjeto(json, tipo));
            }
        }
        return entidades;
    }

    private void observar(String... chaves) {
        if (chaves.length > 0) {
            jedis.watch(chaves);
        }
    }
}
