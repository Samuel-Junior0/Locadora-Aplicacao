package com.locadora.service;

import com.locadora.model.Filme;
import com.locadora.repository.BaseRepository;
import com.locadora.repository.FilmeRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Year;
import java.util.Optional;

public class FilmeService extends BaseService<Filme>{

    private final FilmeRepository filmeRepository;

    public FilmeService(FilmeRepository filmeRepository) {
        super(filmeRepository);
        this.filmeRepository = filmeRepository;
    }

    public Filme cadastrar(String titulo, String genero, Year ano, Duration duracao, BigDecimal valorDiario){
        return filmeRepository.inserir(Filme.novo(titulo, genero, ano, duracao, valorDiario));
    }
    public Filme atualizar(long id, String titulo, String genero, Year ano,
                           Duration duracao, BigDecimal valorDiario) {
        return atualizar(id, new Filme(id, titulo, genero, ano, duracao, valorDiario));
    }

    public Optional<Filme> buscarPorTitulo(String titulo) {
        return filmeRepository.buscarPorTitulo(titulo);
    }
}
