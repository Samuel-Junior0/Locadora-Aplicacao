package com.locadora.app;

import com.locadora.exception.TituloJaExistenteException;
import com.locadora.model.Filme;
import com.locadora.repository.FilmeRepository;
import com.locadora.service.FilmeService;
import com.locadora.util.ConexaoGarnet;
import com.locadora.util.Keys;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Roda só o fluxo de Filme; o fluxo de Cliente fica no {@link Main}. */
public final class MainFilme {

    private MainFilme() {
    }

    public static void main(String[] args) {
        try (Jedis jedis = ConexaoGarnet.abrir()) {
            System.out.printf("Garnet em %s:%d -> %s%n",
                    ConexaoGarnet.host(), ConexaoGarnet.porta(), jedis.ping());

            FilmeService filmes = new FilmeService(new FilmeRepository(jedis));
            List<Long> criados = new ArrayList<>();

            try {
                // sufixo aleatório para não esbarrar em filmes que já existam no banco
                String sufixo = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000), 36);
                String tituloA = "Cidade de Deus " + sufixo;
                String tituloB = "Bacurau " + sufixo;
                String tituloNovoA = "Cidade de Deus (Remasterizado) " + sufixo;

                passo(1, "inserir dois filmes");
                Filme a = filmes.cadastrar(tituloA, "Drama", Year.of(2002),
                        Duration.ofMinutes(130), new BigDecimal("8.5"));
                Filme b = filmes.cadastrar(tituloB, "Suspense", Year.of(2019),
                        Duration.ofMinutes(132), new BigDecimal("9.90"));
                criados.add(a.getId());
                criados.add(b.getId());
                imprimir(a);
                imprimir(b);

                passo(2, "o que o Garnet guardou");
                mostrarChave(jedis, Keys.filme(a.getId()));
                mostrarChave(jedis, Keys.idxFilmeTitulo(a.getTitulo()));
                mostrarChave(jedis, Keys.seqFilme());

                passo(3, "buscar por id e por título (ignorando maiúsculas)");
                Filme lido = filmes.buscarPorId(a.getId()).orElseThrow();
                System.out.println("  id " + a.getId() + " -> " + lido);
                System.out.println("  releitura igual ao original? " + a.equals(lido));
                System.out.println("  \"" + tituloA.toUpperCase() + "\" -> "
                        + filmes.buscarPorTitulo(tituloA.toUpperCase()).orElse(null));

                passo(4, "tentar cadastrar o mesmo título com outra caixa");
                try {
                    filmes.cadastrar(tituloA.toLowerCase(), "Drama", Year.of(2002),
                            Duration.ofMinutes(130), new BigDecimal("8.50"));
                    System.out.println("  ERRO: o título duplicado foi aceito (não deveria)");
                } catch (TituloJaExistenteException e) {
                    System.out.println("  recusado: " + e.getMessage());
                }

                passo(5, "atualizar o título do primeiro filme (o índice antigo tem que sumir)");
                Filme editado = filmes.atualizar(a.getId(), tituloNovoA, "Drama policial",
                        a.getAno(), a.getDuracao(), new BigDecimal("10"));
                imprimir(editado);
                System.out.println("  título antigo -> " + filmes.buscarPorTitulo(tituloA).orElse(null));
                System.out.println("  título novo   -> " + filmes.buscarPorTitulo(tituloNovoA).orElse(null));

                System.out.println("  tentando usar o título do outro filme:");
                try {
                    filmes.atualizar(a.getId(), tituloB, "Drama policial",
                            a.getAno(), a.getDuracao(), new BigDecimal("10"));
                    System.out.println("  ERRO: o título do outro filme foi aceito (não deveria)");
                } catch (TituloJaExistenteException e) {
                    System.out.println("  recusado: " + e.getMessage());
                }

                passo(6, "listar todos");
                filmes.listarTodos().forEach(MainFilme::imprimir);

                passo(7, "remover o segundo filme e conferir que o índice dele sumiu");
                System.out.println("  removeu? " + filmes.remover(b.getId()));
                System.out.println("  busca por id " + b.getId() + " -> "
                        + filmes.buscarPorId(b.getId()).orElse(null));
                mostrarChave(jedis, Keys.idxFilmeTitulo(b.getTitulo()));

                passo(8, "validação: filme com valor diário negativo");
                try {
                    filmes.cadastrar("Inválido " + sufixo, "Drama", Year.of(2000),
                            Duration.ofMinutes(90), new BigDecimal("-1"));
                    System.out.println("  ERRO: o filme inválido foi aceito (não deveria)");
                } catch (IllegalArgumentException e) {
                    System.out.println("  recusado: " + e.getMessage());
                }
            } finally {
                criados.forEach(filmes::remover);
                System.out.printf("%nfilmes restantes no banco: %d%n", filmes.listarTodos().size());
            }
        }
    }

    private static void passo(int numero, String titulo) {
        System.out.printf("%n%d) %s%n", numero, titulo);
    }

    private static void mostrarChave(Jedis jedis, String chave) {
        System.out.println("  " + chave + " = " + jedis.get(chave));
    }

    private static void imprimir(Filme filme) {
        System.out.printf("  id=%d  %s (%s, %s)  %d min  R$ %s/dia%n",
                filme.getId(), filme.getTitulo(), filme.getGenero(), filme.getAno(),
                filme.getDuracao().toMinutes(), filme.getValorDiario());
    }
}