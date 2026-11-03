package com.locadora.menu;

import com.locadora.model.Locacao;
import com.locadora.service.LocacaoService;

import java.util.Optional;
import java.util.Scanner;

public final class MenuLocacao extends MenuBase {

    private final LocacaoService locacoes;

    public MenuLocacao(LocacaoService locacoes, Scanner entrada) {
        super(entrada);
        this.locacoes = locacoes;
    }

    public void executar() {
        while (true) {
            System.out.println("""

                    --- Locações ---
                    1 - Alugar filme
                    2 - Devolver locação
                    3 - Buscar por id
                    4 - Listar abertas
                    5 - Listar devolvidas
                    6 - Listar por cliente
                    7 - Listar por filme
                    8 - Listar todas
                    0 - Voltar
                    """);
            int opcao = lerInteiro("Opção: ");
            if (opcao == 0) {
                return;
            }
            try {
                switch (opcao) {
                    case 1 -> alugar();
                    case 2 -> devolver();
                    case 3 -> buscarPorId();
                    case 4 -> listar(locacoes.listarAbertas(), MenuLocacao::imprimir);
                    case 5 -> listar(locacoes.listarDevolvidas(), MenuLocacao::imprimir);
                    case 6 -> listarPorCliente();
                    case 7 -> listarPorFilme();
                    case 8 -> listar(locacoes.listarTodos(), MenuLocacao::imprimir);
                    default -> System.out.println("  Opção inválida.");
                }
            } catch (RuntimeException e) {
                tratarErro(e);
            }
        }
    }

    private void alugar() {
        long clienteId = lerLong("Id do cliente: ");
        long filmeId = lerLong("Id do filme: ");
        Locacao locacao = locacoes.alugar(clienteId, filmeId);
        System.out.println("  Locação criada:");
        imprimir(locacao);
    }

    private void devolver() {
        long id = lerLong("Id da locação: ");
        Locacao locacao = locacoes.devolver(id);
        System.out.println("  Locação devolvida:");
        imprimir(locacao);
    }

    private void buscarPorId() {
        long id = lerLong("Id: ");
        Optional<Locacao> locacao = locacoes.buscarPorId(id);
        if (locacao.isPresent()) {
            imprimir(locacao.get());
        } else {
            System.out.println("  Locação não encontrada.");
        }
    }

    private void listarPorCliente() {
        long clienteId = lerLong("Id do cliente: ");
        listar(locacoes.listarPorCliente(clienteId), MenuLocacao::imprimir);
    }

    private void listarPorFilme() {
        long filmeId = lerLong("Id do filme: ");
        listar(locacoes.listarPorFilme(filmeId), MenuLocacao::imprimir);
    }

    private static void imprimir(Locacao locacao) {
        String devolucao = locacao.getDataDevolucao() == null
                ? "em aberto"
                : locacao.getDataDevolucao() + " (" + locacao.periodoEmDias() + " dia(s))";

        System.out.printf("  id=%d  cliente=%d  filme=%d  R$ %s/dia  "
                        + "locação=%s  devolução=%s%n",
                locacao.getId(), locacao.getClienteId(), locacao.getFilmeId(),
                locacao.getValorDiaria(), locacao.getDataLocacao(), devolucao);
    }
}
