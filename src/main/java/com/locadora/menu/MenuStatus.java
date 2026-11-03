package com.locadora.menu;

import com.locadora.model.Locacao;
import com.locadora.service.ClienteService;
import com.locadora.service.FilmeService;
import com.locadora.service.LocacaoService;
import com.locadora.service.PagamentoService;
import redis.clients.jedis.Jedis;

import java.util.List;

public final class MenuStatus {

    private final Jedis jedis;
    private final ClienteService clientes;
    private final FilmeService filmes;
    private final LocacaoService locacoes;
    private final PagamentoService pagamentos;

    public MenuStatus(Jedis jedis, ClienteService clientes, FilmeService filmes,
                      LocacaoService locacoes, PagamentoService pagamentos) {
        this.jedis = jedis;
        this.clientes = clientes;
        this.filmes = filmes;
        this.locacoes = locacoes;
        this.pagamentos = pagamentos;
    }

    public void executar() {
        List<Locacao> abertas = locacoes.listarAbertas();
        List<Locacao> devolvidas = locacoes.listarDevolvidas();

        System.out.println("\n--- Status do banco ---");
        System.out.printf("  clientes: %d%n", clientes.listar().size());
        System.out.printf("  filmes: %d%n", filmes.listarTodos().size());
        System.out.printf("  locações: %d (abertas: %d, devolvidas: %d)%n",
                locacoes.listarTodos().size(), abertas.size(), devolvidas.size());
        System.out.printf("  pagamentos: %d%n", pagamentos.listarTodos().size());
        System.out.printf("  total de chaves: %d%n", jedis.dbSize());
    }
}
