package com.locadora.repository;

import com.locadora.exception.EntidadeNaoEncontradaException;
import com.locadora.exception.PagamentoNaoEncontradoException;
import com.locadora.model.Pagamento;
import com.locadora.util.Keys;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class PagamentoRepository extends BaseRepository<Pagamento>{
    

    public PagamentoRepository(Jedis jedis) {
        super(jedis, Pagamento.class, Keys::pagamento, Keys.pagamentosIds(), Keys.seqPagamento());
    }

    @Override
    protected void definirId(Pagamento pagamento, long id) {
        pagamento.setId(id);
    }

    @Override
    protected EntidadeNaoEncontradaException naoEncontrada(long id) {
        return new PagamentoNaoEncontradoException(id);
    }
    
    public Optional<Pagamento> buscarPorLocacao(long locacaoId){
        String id = jedis.get(Keys.idxPagamentoLocacao(locacaoId));
        if (id == null) {
            return Optional.empty();
        }
        return buscarPorId(Long.parseLong(id));
    }

    @Override
    protected String[] chavesIndices(Pagamento pagamento) {
        return new String[]{Keys.idxPagamentoLocacao(pagamento.getLocacaoId())};
    }

    @Override
    protected void gravarIndices(Transaction tx, Pagamento nova, Pagamento atual) {
        tx.set(Keys.idxPagamentoLocacao(nova.getLocacaoId()), String.valueOf(nova.getId()));

        if (atual != null && !atual.getLocacaoId().equals(nova.getLocacaoId())) {
            tx.del(Keys.idxPagamentoLocacao(atual.getLocacaoId()));
        }
    }

    @Override
    protected void apagarIndices(Transaction tx, Pagamento atual) {
        tx.del(Keys.idxPagamentoLocacao(atual.getLocacaoId()));
    }


}
