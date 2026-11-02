package com.locadora.repository;

import com.locadora.exception.EntidadeNaoEncontradaException;
import com.locadora.exception.FilmeIndisponivelException;
import com.locadora.exception.LocacaoNaoEncontradaException;
import com.locadora.model.Locacao;
import com.locadora.util.Keys;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.Objects;

public class LocacaoRepository extends BaseRepository<Locacao> {

    public LocacaoRepository(Jedis jedis) {
        super(jedis, Locacao.class, Keys::locacao, Keys.locacoesIds(), Keys.seqLocacao());
    }

    public List<Locacao> listarAbertas() {
        return listarPorIds(jedis.smembers(Keys.idxLocacaoAberta()));
    }

    public List<Locacao> listarPorCliente(long clienteId) {
        return listarPorIds(jedis.smembers(Keys.idxLocacaoCliente(clienteId)));
    }

    public List<Locacao> listarPorFilme(long filmeId) {
        return listarPorIds(jedis.smembers(Keys.idxLocacaoFilme(filmeId)));
    }

    public List<Locacao> listarDevolvidas() {
        return listarTodos().stream()
                .filter(locacao -> locacao.getDataDevolucao() != null)
                .toList();
    }

    @Override
    protected void definirId(Locacao locacao, long id) {
        locacao.setId(id);
    }

    @Override
    protected EntidadeNaoEncontradaException naoEncontrada(long id) {
        return new LocacaoNaoEncontradaException(id);
    }

    @Override
    protected String[] chavesIndices(Locacao locacao) {
        return new String[]{Keys.idxFilmeDisponivel()};
    }

    @Override
    protected void validarUnicidade(Locacao nova, Locacao atual) {
        if (atual == null && nova.getDataDevolucao() == null
                && !jedis.sismember(Keys.idxFilmeDisponivel(),
                String.valueOf(nova.getFilmeId()))) {
            throw new FilmeIndisponivelException(nova.getFilmeId());
        }
    }

    @Override
    protected void gravarIndices(Transaction tx, Locacao nova, Locacao atual) {
        String id = String.valueOf(nova.getId());

        if (atual == null) {
            tx.sadd(Keys.idxLocacaoCliente(nova.getClienteId()), id);
            tx.sadd(Keys.idxLocacaoFilme(nova.getFilmeId()), id);

            if (nova.getDataDevolucao() == null) {
                tx.sadd(Keys.idxLocacaoAberta(), id);
                tx.srem(Keys.idxFilmeDisponivel(), String.valueOf(nova.getFilmeId()));
            }
            return;
        }

        if (!Objects.equals(atual.getClienteId(), nova.getClienteId())) {
            tx.srem(Keys.idxLocacaoCliente(atual.getClienteId()), id);
            tx.sadd(Keys.idxLocacaoCliente(nova.getClienteId()), id);
        }

        if (!Objects.equals(atual.getFilmeId(), nova.getFilmeId())) {
            tx.srem(Keys.idxLocacaoFilme(atual.getFilmeId()), id);
            tx.sadd(Keys.idxLocacaoFilme(nova.getFilmeId()), id);
        }

        boolean eraAberta = atual.getDataDevolucao() == null;
        boolean estaAberta = nova.getDataDevolucao() == null;

        if (eraAberta && !estaAberta) {
            tx.srem(Keys.idxLocacaoAberta(), id);
            tx.sadd(Keys.idxFilmeDisponivel(), String.valueOf(nova.getFilmeId()));
        } else if (!eraAberta && estaAberta) {
            tx.sadd(Keys.idxLocacaoAberta(), id);
            tx.srem(Keys.idxFilmeDisponivel(), String.valueOf(nova.getFilmeId()));
        }
    }

    @Override
    protected void apagarIndices(Transaction tx, Locacao atual) {
        String id = String.valueOf(atual.getId());

        tx.srem(Keys.idxLocacaoCliente(atual.getClienteId()), id);
        tx.srem(Keys.idxLocacaoFilme(atual.getFilmeId()), id);
        tx.srem(Keys.idxLocacaoAberta(), id);

        if (atual.getDataDevolucao() == null) {
            tx.sadd(Keys.idxFilmeDisponivel(), String.valueOf(atual.getFilmeId()));
        }
    }
}
