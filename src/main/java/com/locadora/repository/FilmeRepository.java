package com.locadora.repository;

import com.locadora.exception.*;
import com.locadora.model.Cliente;
import com.locadora.model.Filme;
import com.locadora.util.Json;
import com.locadora.util.Keys;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Year;
import java.util.Optional;

public class FilmeRepository extends BaseRepository<Filme>{

    public FilmeRepository(Jedis jedis) {
        super(jedis, Filme.class, Keys::filme, Keys.filmesIds() , Keys.seqFilme());
    }

    public Optional<Filme> buscarPorTitulo(String titulo){
        String id = jedis.get(Keys.idxFilmeTitulo(titulo));
        if (id == null){
            return Optional.empty();
        }
        return buscarPorId(Long.parseLong(id));
    }

    @Override
    protected void definirId(Filme filme, long id) {
        filme.setId(id);
    }

    @Override
    protected EntidadeNaoEncontradaException naoEncontrada(long id) {
        return new FilmeNaoEncontradoException(id);
    }
    @Override
    protected String[] chavesIndices(Filme filme) {
        return new String[]{Keys.idxFilmeTitulo(filme.getTitulo())};
    }

    @Override
    protected void validarUnicidade(Filme nova, Filme atual) {
        String dono = jedis.get(Keys.idxFilmeTitulo(nova.getTitulo()));
        boolean ocupadoPorOutro = dono != null
                && (atual == null || !dono.equals(String.valueOf(atual.getId())));
        if (ocupadoPorOutro) {
            throw new TituloJaExistenteException(nova.getTitulo());
        }
    }

    @Override
    protected void gravarIndices(Transaction tx, Filme nova, Filme atual) {
        tx.set(Keys.idxFilmeTitulo(nova.getTitulo()), String.valueOf(nova.getId()));

        if (atual == null) {
            tx.sadd(Keys.idxFilmeDisponivel(), String.valueOf(nova.getId()));
        }

        if (atual != null && !Keys.idxFilmeTitulo(atual.getTitulo())
                .equals(Keys.idxFilmeTitulo(nova.getTitulo()))) {
            tx.del(Keys.idxFilmeTitulo(atual.getTitulo()));
        }
    }

    @Override
    protected void apagarIndices(Transaction tx, Filme atual) {
        tx.del(Keys.idxFilmeTitulo(atual.getTitulo()));
        tx.srem(Keys.idxFilmeDisponivel(), String.valueOf(atual.getId()));
    }
}
