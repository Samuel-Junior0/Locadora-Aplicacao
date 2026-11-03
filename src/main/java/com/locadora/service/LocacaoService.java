package com.locadora.service;

import com.locadora.exception.ClienteNaoEncontradoException;
import com.locadora.exception.FilmeNaoEncontradoException;
import com.locadora.exception.LocacaoJaDevolvidaException;
import com.locadora.exception.LocacaoNaoEncontradaException;
import com.locadora.model.Cliente;
import com.locadora.model.Filme;
import com.locadora.model.Locacao;
import com.locadora.repository.ClienteRepository;
import com.locadora.repository.FilmeRepository;
import com.locadora.repository.LocacaoRepository;
import com.locadora.util.ValidadorLocacao;

import java.time.LocalDate;
import java.util.List;

public final class LocacaoService extends BaseService<Locacao> {

    private final LocacaoRepository locacaoRepository;
    private final ClienteRepository clienteRepository;
    private final FilmeRepository filmeRepository;

    public LocacaoService(LocacaoRepository locacaoRepository,
                          ClienteRepository clienteRepository,
                          FilmeRepository filmeRepository) {
        super(locacaoRepository);
        this.locacaoRepository = locacaoRepository;
        this.clienteRepository = clienteRepository;
        this.filmeRepository = filmeRepository;
    }

    public Locacao alugar(long clienteId, long filmeId) {
        Cliente cliente = clienteRepository.buscarPorId(clienteId)
                .orElseThrow(() -> new ClienteNaoEncontradoException(clienteId));

        Filme filme = filmeRepository.buscarPorId(filmeId)
                .orElseThrow(() -> new FilmeNaoEncontradoException(filmeId));

        Locacao locacao = Locacao.nova(
                cliente.getId(),
                filme.getId(),
                filme.getValorDiario()
        );

        return locacaoRepository.inserir(locacao);
    }

    public Locacao devolver(long locacaoId) {
        return devolver(locacaoId, LocalDate.now());
    }

    public Locacao devolver(long locacaoId, LocalDate dataDevolucao) {
        Locacao locacao = locacaoRepository.buscarPorId(locacaoId)
                .orElseThrow(() -> new LocacaoNaoEncontradaException(locacaoId));

        if (locacao.getDataDevolucao() != null) {
            throw new LocacaoJaDevolvidaException(locacaoId);
        }

        locacao.setDataDevolucao(dataDevolucao);
        ValidadorLocacao.validarDevolucao(locacao);

        return locacaoRepository.atualizar(locacaoId, locacao);
    }

    public List<Locacao> listarAbertas() {
        return locacaoRepository.listarAbertas();
    }

    public List<Locacao> listarPorCliente(long clienteId) {
        return locacaoRepository.listarPorCliente(clienteId);
    }

    public List<Locacao> listarPorFilme(long filmeId) {
        return locacaoRepository.listarPorFilme(filmeId);
    }

    public List<Locacao> listarDevolvidas() {
        return locacaoRepository.listarDevolvidas();
    }

    @Override
    public Locacao atualizar(long id, Locacao locacao) {
        throw new UnsupportedOperationException(
                "Use alugar() ou devolver() para alterar uma locação");
    }
}
