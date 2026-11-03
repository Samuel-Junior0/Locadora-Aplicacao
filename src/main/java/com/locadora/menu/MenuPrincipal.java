package com.locadora.menu;

import com.locadora.repository.PagamentoRepository;
import com.locadora.service.ClienteService;
import com.locadora.service.FilmeService;
import com.locadora.service.LocacaoService;
import com.locadora.service.PagamentoService;
import redis.clients.jedis.Jedis;

import java.util.Scanner;

public final class MenuPrincipal extends MenuBase {

    private final MenuCliente menuClientes;
    private final MenuFilme menuFilmes;
    private final MenuLocacao menuLocacoes;
    private final MenuPagamento menuPagamentos;
    private final MenuStatus menuStatus;

    public MenuPrincipal(Scanner entrada, Jedis jedis, ClienteService clientes,
                         FilmeService filmes, LocacaoService locacoes,
                         PagamentoService pagamentos, PagamentoRepository pagamentoRepository) {
        super(entrada);
        this.menuClientes = new MenuCliente(clientes, entrada);
        this.menuFilmes = new MenuFilme(filmes, entrada);
        this.menuLocacoes = new MenuLocacao(locacoes, entrada);
        this.menuPagamentos = new MenuPagamento(locacoes, pagamentos, pagamentoRepository, entrada);
        this.menuStatus = new MenuStatus(jedis, clientes, filmes, locacoes, pagamentos);
    }

    public void executar() {
        try {
            while (true) {
                System.out.println("""

                        ===== LOCADORA =====
                        1 - Clientes
                        2 - Filmes
                        3 - Locações
                        4 - Pagamentos
                        5 - Status do banco
                        0 - Sair
                        """);
                int opcao = lerInteiro("Escolha: ");

                try {
                    switch (opcao) {
                        case 1 -> menuClientes.executar();
                        case 2 -> menuFilmes.executar();
                        case 3 -> menuLocacoes.executar();
                        case 4 -> menuPagamentos.executar();
                        case 5 -> menuStatus.executar();
                        case 0 -> {
                            System.out.println("Até logo!");
                            return;
                        }
                        default -> System.out.println("  Opção inválida.");
                    }
                } catch (RuntimeException e) {
                    tratarErro(e);
                }
            }
        } catch (FimDaEntradaException e) {
            System.out.println("\nEntrada encerrada. Até logo!");
        }
    }
}
