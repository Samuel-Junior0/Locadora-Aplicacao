package com.locadora.menu;

import com.locadora.model.Cliente;
import com.locadora.service.ClienteService;
import com.locadora.util.Cpf;

import java.util.Optional;
import java.util.Scanner;

public final class MenuCliente extends MenuBase {

    private final ClienteService clientes;

    public MenuCliente(ClienteService clientes, Scanner entrada) {
        super(entrada);
        this.clientes = clientes;
    }

    public void executar() {
        while (true) {
            System.out.println("""

                    --- Clientes ---
                    1 - Cadastrar
                    2 - Buscar por id
                    3 - Buscar por CPF
                    4 - Listar todos
                    5 - Atualizar
                    6 - Remover
                    0 - Voltar
                    """);
            int opcao = lerInteiro("Opção: ");
            if (opcao == 0) {
                return;
            }
            try {
                switch (opcao) {
                    case 1 -> cadastrar();
                    case 2 -> buscarPorId();
                    case 3 -> buscarPorCpf();
                    case 4 -> listar(clientes.listar(), MenuCliente::imprimir);
                    case 5 -> atualizar();
                    case 6 -> remover();
                    default -> System.out.println("  Opção inválida.");
                }
            } catch (RuntimeException e) {
                tratarErro(e);
            }
        }
    }

    private void cadastrar() {
        String nome = ler("Nome: ");
        String cpf = ler("CPF: ");
        Cliente cliente = clientes.cadastrar(nome, cpf);
        System.out.println("  Cliente cadastrado:");
        imprimir(cliente);
    }

    private void buscarPorId() {
        long id = lerLong("Id: ");
        Optional<Cliente> cliente = clientes.buscarPorId(id);
        if (cliente.isPresent()) {
            imprimir(cliente.get());
        } else {
            System.out.println("  Cliente não encontrado.");
        }
    }

    private void buscarPorCpf() {
        String cpf = ler("CPF: ");
        clientes.buscarPorCpf(cpf).ifPresentOrElse(
                MenuCliente::imprimir,
                () -> System.out.println("  Cliente não encontrado."));
    }

    private void atualizar() {
        long id = lerLong("Id do cliente: ");
        Optional<Cliente> encontrado = clientes.buscarPorId(id);
        if (encontrado.isEmpty()) {
            System.out.println("  Cliente não encontrado.");
            return;
        }
        Cliente atual = encontrado.get();
        System.out.println("  Atual:");
        imprimir(atual);

        String nome = lerComPadrao("Nome", atual.getNome());
        String cpf = lerComPadrao("CPF", Cpf.formatar(atual.getCpf()));
        Cliente atualizado = clientes.atualizar(id, nome, cpf);
        System.out.println("  Cliente atualizado:");
        imprimir(atualizado);
    }

    private void remover() {
        long id = lerLong("Id do cliente: ");
        boolean removeu = clientes.remover(id);
        System.out.println(removeu ? "  Cliente removido." : "  Cliente não encontrado.");
    }

    private static void imprimir(Cliente cliente) {
        System.out.printf("  id=%d  nome=%s  cpf=%s%n",
                cliente.getId(), cliente.getNome(), Cpf.formatar(cliente.getCpf()));
    }
}
