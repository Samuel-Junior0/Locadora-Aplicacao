package com.locadora.menu;

import com.locadora.model.Filme;
import com.locadora.service.FilmeService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Year;
import java.util.Optional;
import java.util.Scanner;

public final class MenuFilme extends MenuBase {

    private final FilmeService filmes;

    public MenuFilme(FilmeService filmes, Scanner entrada) {
        super(entrada);
        this.filmes = filmes;
    }

    public void executar() {
        while (true) {
            System.out.println("""

                    --- Filmes ---
                    1 - Cadastrar
                    2 - Buscar por id
                    3 - Buscar por título
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
                    case 3 -> buscarPorTitulo();
                    case 4 -> listar(filmes.listarTodos(), MenuFilme::imprimir);
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
        String titulo = ler("Título: ");
        String genero = ler("Gênero: ");
        int ano = lerInteiro("Ano: ");
        long minutos = lerLong("Duração (min): ");
        BigDecimal valorDiario = lerValor("Valor diário: R$ ");
        Filme filme = filmes.cadastrar(titulo, genero, Year.of(ano),
                Duration.ofMinutes(minutos), valorDiario);
        System.out.println("  Filme cadastrado:");
        imprimir(filme);
    }

    private void buscarPorId() {
        long id = lerLong("Id: ");
        Optional<Filme> filme = filmes.buscarPorId(id);
        if (filme.isPresent()) {
            imprimir(filme.get());
        } else {
            System.out.println("  Filme não encontrado.");
        }
    }

    private void buscarPorTitulo() {
        String titulo = ler("Título: ");
        filmes.buscarPorTitulo(titulo).ifPresentOrElse(
                MenuFilme::imprimir,
                () -> System.out.println("  Filme não encontrado."));
    }

    private void atualizar() {
        long id = lerLong("Id do filme: ");
        Optional<Filme> encontrado = filmes.buscarPorId(id);
        if (encontrado.isEmpty()) {
            System.out.println("  Filme não encontrado.");
            return;
        }
        Filme atual = encontrado.get();
        System.out.println("  Atual:");
        imprimir(atual);

        String titulo = lerComPadrao("Título", atual.getTitulo());
        String genero = lerComPadrao("Gênero", atual.getGenero());
        int ano = lerInteiroComPadrao("Ano", atual.getAno().getValue());
        long minutos = lerLongComPadrao("Duração (min)", atual.getDuracao().toMinutes());
        BigDecimal valorDiario = lerValorComPadrao("Valor diário", atual.getValorDiario());

        Filme atualizado = filmes.atualizar(id, titulo, genero, Year.of(ano),
                Duration.ofMinutes(minutos), valorDiario);
        System.out.println("  Filme atualizado:");
        imprimir(atualizado);
    }

    private void remover() {
        long id = lerLong("Id do filme: ");
        boolean removeu = filmes.remover(id);
        System.out.println(removeu ? "  Filme removido." : "  Filme não encontrado.");
    }

    private static void imprimir(Filme filme) {
        System.out.printf("  id=%d  %s (%s, %s)  %d min  R$ %s/dia%n",
                filme.getId(), filme.getTitulo(), filme.getGenero(), filme.getAno(),
                filme.getDuracao().toMinutes(), filme.getValorDiario());
    }
}
