package com.locadora.app;

import com.locadora.menu.MenuPrincipal;
import com.locadora.repository.ClienteRepository;
import com.locadora.repository.FilmeRepository;
import com.locadora.repository.LocacaoRepository;
import com.locadora.repository.PagamentoRepository;
import com.locadora.service.ClienteService;
import com.locadora.service.FilmeService;
import com.locadora.service.LocacaoService;
import com.locadora.service.PagamentoService;
import com.locadora.util.ConexaoGarnet;
import redis.clients.jedis.Jedis;

import java.util.Scanner;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try (Jedis jedis = ConexaoGarnet.abrir()) {
            System.out.printf("Conectado ao Garnet em %s:%d -> %s%n",
                    ConexaoGarnet.host(), ConexaoGarnet.porta(), jedis.ping());

            ClienteRepository clienteRepository = new ClienteRepository(jedis);
            FilmeRepository filmeRepository = new FilmeRepository(jedis);
            LocacaoRepository locacaoRepository = new LocacaoRepository(jedis);
            PagamentoRepository pagamentoRepository = new PagamentoRepository(jedis);

            ClienteService clientes = new ClienteService(clienteRepository);
            FilmeService filmes = new FilmeService(filmeRepository);
            LocacaoService locacoes =
                    new LocacaoService(locacaoRepository, clienteRepository, filmeRepository);
            PagamentoService pagamentos =
                    new PagamentoService(locacoes, pagamentoRepository, locacaoRepository);

            new MenuPrincipal(new Scanner(System.in), jedis, clientes, filmes,
                    locacoes, pagamentos, pagamentoRepository).executar();
        } catch (RuntimeException e) {
            String mensagem = e.getMessage();
            System.out.println("Falha ao conectar ou executar: "
                    + (mensagem == null || mensagem.isBlank()
                    ? e.getClass().getSimpleName()
                    : mensagem));
        }
    }
}
