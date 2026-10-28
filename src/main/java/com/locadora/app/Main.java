package com.locadora.app;

import com.locadora.exception.CpfJaCadastradoException;
import com.locadora.model.Cliente;
import com.locadora.repository.ClienteRepository;
import com.locadora.service.ClienteService;
import com.locadora.util.ConexaoGarnet;
import com.locadora.util.Cpf;
import com.locadora.util.Keys;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try (Jedis jedis = ConexaoGarnet.abrir()) {
            System.out.printf("Garnet em %s:%d -> %s%n",
                    ConexaoGarnet.host(), ConexaoGarnet.porta(), jedis.ping());

            ClienteService clientes =
                    new ClienteService(new ClienteRepository(jedis));
            List<Long> criados = new ArrayList<>();

            try {
                String cpfAna = cpfAleatorio();
                String cpfBruno = cpfAleatorio();
                String cpfNovoAna = cpfAleatorio();

                passo(1, "inserir dois clientes");
                Cliente ana = clientes.cadastrar("Ana Souza", cpfAna);
                Cliente bruno = clientes.cadastrar("Bruno Lima", cpfBruno);
                criados.add(ana.getId());
                criados.add(bruno.getId());
                imprimir(ana);
                imprimir(bruno);

                passo(2, "o que o Garnet guardou");
                System.out.println("  " + Keys.cliente(ana.getId()) + " = "
                        + jedis.get(Keys.cliente(ana.getId())));
                System.out.println("  " + Keys.idxClienteCpf(ana.getCpf()) + " = "
                        + jedis.get(Keys.idxClienteCpf(ana.getCpf())));
                System.out.println("  " + Keys.seqCliente() + " = "
                        + jedis.get(Keys.seqCliente()));

                passo(3, "buscar por id e por CPF (com máscara)");
                System.out.println("  id " + ana.getId() + " -> "
                        + clientes.buscarPorId(ana.getId()).orElse(null));
                System.out.println("  " + Cpf.formatar(cpfAna) + " -> "
                        + clientes.buscarPorCpf(Cpf.formatar(cpfAna)).orElse(null));

                passo(4, "tentar cadastrar o CPF da Ana de novo");
                try {
                    clientes.cadastrar("Ana Clone", cpfAna);
                    System.out.println("  ERRO: o CPF duplicado foi aceito (não deveria)");
                } catch (CpfJaCadastradoException e) {
                    System.out.println("  recusado: " + e.getMessage());
                }

                passo(5, "atualizar nome e CPF da Ana (o índice antigo tem que sumir)");
                Cliente anaEditada =
                        clientes.atualizar(ana.getId(), "Ana Souza Silva", cpfNovoAna);
                imprimir(anaEditada);
                System.out.println("  CPF antigo " + Cpf.formatar(cpfAna) + " -> "
                        + clientes.buscarPorCpf(cpfAna).orElse(null));
                System.out.println("  CPF novo   " + Cpf.formatar(cpfNovoAna) + " -> "
                        + clientes.buscarPorCpf(cpfNovoAna).orElse(null));

                System.out.println("  tentando usar o CPF do Bruno:");
                try {
                    clientes.atualizar(ana.getId(), "Ana Souza Silva", cpfBruno);
                    System.out.println("  ERRO: o CPF do Bruno foi aceito (não deveria)");
                } catch (CpfJaCadastradoException e) {
                    System.out.println("  recusado: " + e.getMessage());
                }

                passo(6, "listar todos");
                for (Cliente cliente : clientes.listar()) {
                    imprimir(cliente);
                }

                passo(7, "remover o Bruno e conferir que o índice dele sumiu");
                System.out.println("  removeu? " + clientes.remover(bruno.getId()));
                System.out.println("  busca por id " + bruno.getId() + " -> "
                        + clientes.buscarPorId(bruno.getId()).orElse(null));
                System.out.println("  " + Keys.idxClienteCpf(bruno.getCpf()) + " -> "
                        + jedis.get(Keys.idxClienteCpf(bruno.getCpf())));
            } finally {
                criados.forEach(clientes::remover);
                System.out.printf("%nclientes restantes no banco: %d%n",
                        clientes.listar().size());
            }
        }
    }

    private static void passo(int numero, String titulo) {
        System.out.printf("%n%d) %s%n", numero, titulo);
    }

    private static void imprimir(Cliente cliente) {
        System.out.printf("  id=%d  nome=%s  cpf=%s%n",
                cliente.getId(), cliente.getNome(), Cpf.formatar(cliente.getCpf()));
    }

    private static String cpfAleatorio() {
        return String.format("%011d", ThreadLocalRandom.current().nextLong(100_000_000_000L));
    }
}
