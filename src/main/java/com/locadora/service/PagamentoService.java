package com.locadora.service;

import com.locadora.exception.EntidadeNaoEncontradaException;
import com.locadora.exception.LocacaoNaoDevolvidaException;
import com.locadora.exception.LocacaoNaoEncontradaException;
import com.locadora.exception.NenhumaLocacaoSemPagamentoException;
import com.locadora.model.Locacao;
import com.locadora.model.Pagamento;
import com.locadora.repository.BaseRepository;
import com.locadora.repository.LocacaoRepository;
import com.locadora.repository.PagamentoRepository;
import com.locadora.util.CodigoPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class PagamentoService extends BaseService<Pagamento>{

    private final LocacaoService locacaoService;

    private final PagamentoRepository pagamentoRepository;
    private final LocacaoRepository locacaoRepository;

    public PagamentoService(LocacaoService locacaoService, PagamentoRepository pagamentoRepository, LocacaoRepository locacaoRepository) {
        super(pagamentoRepository);
        this.locacaoService = locacaoService;
        this.pagamentoRepository = pagamentoRepository;
        this.locacaoRepository = locacaoRepository;
    }

    public Pagamento gerar(long locacaoId, BigDecimal valorDiario){

        Optional<Locacao> locacao = locacaoRepository.buscarPorId(locacaoId);

        if (locacao.isEmpty()){
            throw new LocacaoNaoEncontradaException(locacaoId);
        }

        if (locacao.get().getDataLocacao() == null){
            throw new LocacaoNaoDevolvidaException(locacao.get().getId());
        }

        if(locacao.get().getDataDevolucao() == null){
            throw new LocacaoNaoDevolvidaException(locacao.get().getId());
        }

        long dias = ChronoUnit.DAYS.between(locacao.get().getDataLocacao(),locacao.get().getDataDevolucao());
        long periodoPagamento = Math.max(1, dias); // Usado para garantir que o cliente sempre vai pagar no minimo 1 dia.

        BigDecimal valorTotal = valorDiario.multiply(BigDecimal.valueOf(periodoPagamento));

        Pagamento novoPagamento = new Pagamento(null, locacaoId, valorTotal, LocalDate.now(), new CodigoPagamento());

        return pagamentoRepository.inserir(novoPagamento);
    }

    Optional<Pagamento> buscarPorLocacao(long locacaoId){
        return pagamentoRepository.buscarPorLocacao(locacaoId);
    }

    List<Locacao> locacoesDevolvidasSemPagamento(){

        List<Locacao> locacoes = locacaoService.listarDevolvidas();
        List<Locacao> locacoesDevolvidasSemPagamento = new ArrayList<>();

        for (Locacao locacao: locacoes){
            if(locacao.getDataDevolucao() != null && buscarPorLocacao(locacao.getId()).isEmpty()){
               locacoesDevolvidasSemPagamento.add(locacao);
            }
        }

        if (locacoesDevolvidasSemPagamento.isEmpty()){
            throw new NenhumaLocacaoSemPagamentoException("Nenhuma Locação sme pagamento");
        }

        return locacoesDevolvidasSemPagamento;
    }

}
