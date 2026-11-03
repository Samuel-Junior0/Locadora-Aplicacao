package com.locadora.app;

import com.locadora.exception.ClienteNaoEncontradoException;
import com.locadora.exception.DataInvalidaException;
import com.locadora.exception.FilmeIndisponivelException;
import com.locadora.exception.FilmeNaoEncontradoException;
import com.locadora.exception.LocacaoJaDevolvidaException;
import com.locadora.exception.LocacaoNaoDevolvidaException;
import com.locadora.exception.LocacaoNaoEncontradaException;
import com.locadora.model.Cliente;
import com.locadora.model.Filme;
import com.locadora.model.Locacao;
import com.locadora.model.Pagamento;
import com.locadora.repository.ClienteRepository;
import com.locadora.repository.FilmeRepository;
import com.locadora.repository.LocacaoRepository;
import com.locadora.repository.PagamentoRepository;
import com.locadora.service.ClienteService;
import com.locadora.service.FilmeService;
import com.locadora.service.LocacaoService;
import com.locadora.service.PagamentoService;
import com.locadora.util.ConexaoGarnet;
import com.locadora.util.Keys;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Testa o fluxo completo da locadora: cliente -> filme -> locação -> devolução -> pagamento.
 * Cada verificação imprime OK ou ERRO e o resumo final mostra quantas falharam.
 */
public final class MainLocadora {

    private static int verificacoes = 0;
    private static int falhas = 0;

    private MainLocadora() {
    }

    public static void main(String[] args) {
        try (Jedis jedis = ConexaoGarnet.abrir()) {
            System.out.printf("Garnet em %s:%d -> %s%n",
                    ConexaoGarnet.host(), ConexaoGarnet.porta(), jedis.ping());

            ClienteRepository clienteRepo = new ClienteRepository(jedis);
            FilmeRepository filmeRepo = new FilmeRepository(jedis);
            LocacaoRepository locacaoRepo = new LocacaoRepository(jedis);
            PagamentoRepository pagamentoRepo = new PagamentoRepository(jedis);

            ClienteService clientes = new ClienteService(clienteRepo);
            FilmeService filmes = new FilmeService(filmeRepo);
            LocacaoService locacoes = new LocacaoService(locacaoRepo, clienteRepo, filmeRepo);
            PagamentoService pagamentos = new PagamentoService(locacoes, pagamentoRepo, locacaoRepo);

            List<Long> clientesCriados = new ArrayList<>();
            List<Long> filmesCriados = new ArrayList<>();
            List<Long> locacoesCriadas = new ArrayList<>();
            List<Long> pagamentosCriados = new ArrayList<>();

            try {
                LocalDate hoje = LocalDate.now();
                String sufixo = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000), 36);
                BigDecimal valorDiario = new BigDecimal("8.50");

                passo(1, "preparar um cliente e um filme");
                Cliente ana = clientes.cadastrar("Ana Souza", cpfAleatorio());
                Filme filme = filmes.cadastrar("Locação Teste " + sufixo, "Drama", Year.of(2002),
                        Duration.ofMinutes(130), valorDiario);
                clientesCriados.add(ana.getId());
                filmesCriados.add(filme.getId());
                System.out.println("  " + ana);
                System.out.println("  " + filme);
                conferir("filme começa disponível",
                        jedis.sismember(Keys.idxFilmeDisponivel(), String.valueOf(filme.getId())));

                passo(2, "alugar o filme");
                Locacao loc1 = locacoes.alugar(ana.getId(), filme.getId());
                locacoesCriadas.add(loc1.getId());
                System.out.println("  " + loc1);
                conferir("id da locação foi gerado", loc1.getId() != null);
                conferir("data da locação é hoje", hoje.equals(loc1.getDataLocacao()));
                conferir("sem data de devolução", loc1.getDataDevolucao() == null);
                conferir("diária copiada do filme",
                        valorDiario.compareTo(loc1.getValorDiaria()) == 0);

                passo(3, "o que o Garnet guardou");
                mostrarChave(jedis, Keys.locacao(loc1.getId()));
                conferir("filme saiu de 'disponível'",
                        !jedis.sismember(Keys.idxFilmeDisponivel(), String.valueOf(filme.getId())));
                conferir("locação está em 'abertas'",
                        jedis.sismember(Keys.idxLocacaoAberta(), String.valueOf(loc1.getId())));
                conferir("locação indexada no cliente",
                        jedis.sismember(Keys.idxLocacaoCliente(ana.getId()), String.valueOf(loc1.getId())));
                conferir("locação indexada no filme",
                        jedis.sismember(Keys.idxLocacaoFilme(filme.getId()), String.valueOf(loc1.getId())));

                passo(4, "alugar quando não pode");
                esperarErro("mesmo filme já alugado", FilmeIndisponivelException.class,
                        () -> locacoes.alugar(ana.getId(), filme.getId()));
                esperarErro("cliente inexistente", ClienteNaoEncontradoException.class,
                        () -> locacoes.alugar(999_999_999L, filme.getId()));
                esperarErro("filme inexistente", FilmeNaoEncontradoException.class,
                        () -> locacoes.alugar(ana.getId(), 999_999_999L));

                passo(5, "consultar locações");
                conferir("buscarPorId devolve a mesma locação",
                        locacoes.buscarPorId(loc1.getId()).map(loc1::equals).orElse(false));
                conferir("listarAbertas contém a locação", contem(locacoes.listarAbertas(), loc1.getId()));
                conferir("listarPorCliente contém a locação",
                        contem(locacoes.listarPorCliente(ana.getId()), loc1.getId()));
                conferir("listarPorFilme contém a locação",
                        contem(locacoes.listarPorFilme(filme.getId()), loc1.getId()));
                conferir("ainda não está em listarDevolvidas",
                        !contem(locacoes.listarDevolvidas(), loc1.getId()));

                passo(6, "gerar pagamento de locação ainda aberta (tem que ser recusado)");
                esperarErro("pagamento antes da devolução", LocacaoNaoDevolvidaException.class,
                        () -> pagamentos.gerar(loc1.getId(), valorDiario));
                esperarErro("pagamento de locação inexistente", LocacaoNaoEncontradaException.class,
                        () -> pagamentos.gerar(999_999_999L, valorDiario));

                passo(7, "devolver 3 dias depois");
                LocalDate devolucao = hoje.plusDays(3);
                Locacao devolvida = locacoes.devolver(loc1.getId(), devolucao);
                System.out.println("  " + devolvida);
                conferir("data de devolução gravada", devolucao.equals(devolvida.getDataDevolucao()));
                conferir("filme voltou a ficar disponível",
                        jedis.sismember(Keys.idxFilmeDisponivel(), String.valueOf(filme.getId())));
                conferir("saiu de 'abertas'", !contem(locacoes.listarAbertas(), loc1.getId()));
                conferir("entrou em listarDevolvidas", contem(locacoes.listarDevolvidas(), loc1.getId()));
                esperarErro("devolver duas vezes", LocacaoJaDevolvidaException.class,
                        () -> locacoes.devolver(loc1.getId()));
                esperarErro("devolver locação inexistente", LocacaoNaoEncontradaException.class,
                        () -> locacoes.devolver(999_999_999L));
                esperarErro("atualizar locação direto", UnsupportedOperationException.class,
                        () -> locacoes.atualizar(loc1.getId(), devolvida));

                passo(8, "segunda locação: devolução com data inválida e devolução no mesmo dia");
                Locacao loc2 = locacoes.alugar(ana.getId(), filme.getId());
                locacoesCriadas.add(loc2.getId());
                System.out.println("  " + loc2);
                esperarErro("devolver antes de ter alugado", DataInvalidaException.class,
                        () -> locacoes.devolver(loc2.getId(), hoje.minusDays(1)));
                conferir("locação continua aberta depois da recusa",
                        locacoes.buscarPorId(loc2.getId())
                                .map(l -> l.getDataDevolucao() == null).orElse(false));
                conferir("filme continua indisponível depois da recusa",
                        !jedis.sismember(Keys.idxFilmeDisponivel(), String.valueOf(filme.getId())));
                Locacao devolvida2 = locacoes.devolver(loc2.getId());
                System.out.println("  " + devolvida2);
                conferir("devolvida hoje", hoje.equals(devolvida2.getDataDevolucao()));

                passo(9, "gerar pagamentos");
                Pagamento pag1 = pagamentos.gerar(loc1.getId(), valorDiario);
                pagamentosCriados.add(pag1.getId());
                imprimir(pag1);
                conferir("3 dias x 8.50 = 25.50",
                        new BigDecimal("25.50").compareTo(pag1.getValor()) == 0);

                Pagamento pag2 = pagamentos.gerar(loc2.getId(), valorDiario);
                pagamentosCriados.add(pag2.getId());
                imprimir(pag2);
                conferir("devolução no mesmo dia cobra o mínimo de 1 dia (8.50)",
                        valorDiario.compareTo(pag2.getValor()) == 0);
                conferir("códigos de pagamento são diferentes",
                        !pag1.getCodigoPagamento().getCodigoGerado()
                                .equals(pag2.getCodigoPagamento().getCodigoGerado()));

                passo(10, "reler os pagamentos do Garnet");
                mostrarChave(jedis, Keys.pagamento(pag1.getId()));
                Optional<Pagamento> relido = pagamentoRepo.buscarPorId(pag1.getId());
                conferir("buscarPorId encontra o pagamento", relido.isPresent());
                relido.ifPresent(p -> {
                    conferir("locação do pagamento preservada", loc1.getId().equals(p.getLocacaoId()));
                    conferir("valor preservado", pag1.getValor().compareTo(p.getValor()) == 0);
                    conferir("data preservada", pag1.getDataPagamento().equals(p.getDataPagamento()));
                    conferir("código de pagamento preservado após reler",
                            pag1.getCodigoPagamento().getCodigoGerado()
                                    .equals(p.getCodigoPagamento().getCodigoGerado()));
                });

                Optional<Pagamento> porLocacao = pagamentoRepo.buscarPorLocacao(loc2.getId());
                conferir("buscarPorLocacao devolve o pagamento da locação " + loc2.getId(),
                        porLocacao.map(p -> loc2.getId().equals(p.getLocacaoId())).orElse(false));

                passo(11, "listar pagamentos");
                List<Pagamento> todos = pagamentos.listarTodos();
                System.out.println("  total no banco: " + todos.size());
                conferir("listarTodos contém os dois pagamentos",
                        contem(todos, pag1.getId()) && contem(todos, pag2.getId()));
            } finally {
                System.out.println();
                limpar("pagamento", pagamentosCriados, pagamentos::remover);
                limpar("locação", locacoesCriadas, locacoes::remover);
                limpar("filme", filmesCriados, filmes::remover);
                limpar("cliente", clientesCriados, clientes::remover);
            }
        } finally {
            System.out.printf("%n== %d verificações, %d com ERRO ==%n", verificacoes, falhas);
        }
    }

    /** Remove cada id criado pelo teste; uma falha é impressa e não interrompe o resto. */
    private static void limpar(String tipo, List<Long> ids,
                               java.util.function.LongPredicate remover) {
        for (Long id : ids) {
            try {
                remover.test(id);
            } catch (RuntimeException e) {
                System.out.printf("  não consegui remover %s %d: %s%n", tipo, id, e.getMessage());
            }
        }
    }

    private static void passo(int numero, String titulo) {
        System.out.printf("%n%d) %s%n", numero, titulo);
    }

    private static void conferir(String descricao, boolean ok) {
        verificacoes++;
        if (!ok) {
            falhas++;
        }
        System.out.println("  " + (ok ? "OK   " : "ERRO ") + descricao);
    }

    private static void esperarErro(String descricao, Class<? extends RuntimeException> esperada,
                                    Runnable acao) {
        verificacoes++;
        try {
            acao.run();
            falhas++;
            System.out.println("  ERRO " + descricao + ": foi aceito (não deveria)");
        } catch (RuntimeException e) {
            if (esperada.isInstance(e)) {
                System.out.println("  OK   " + descricao + " -> recusado: " + e.getMessage());
            } else {
                falhas++;
                System.out.println("  ERRO " + descricao + ": esperava "
                        + esperada.getSimpleName() + " mas veio " + e.getClass().getSimpleName()
                        + (e.getMessage() == null ? "" : " (" + e.getMessage() + ")"));
            }
        }
    }

    private static void mostrarChave(Jedis jedis, String chave) {
        System.out.println("  " + chave + " = " + jedis.get(chave));
    }

    private static void imprimir(Pagamento p) {
        System.out.printf("  pagamento id=%d  locação=%d  valor=R$ %s  data=%s  código=%s%n",
                p.getId(), p.getLocacaoId(), p.getValor(), p.getDataPagamento(),
                p.getCodigoPagamento().getCodigoGerado());
    }

    private static boolean contem(List<?> lista, Long id) {
        return lista.stream().anyMatch(item -> {
            if (item instanceof Locacao l) {
                return id.equals(l.getId());
            }
            if (item instanceof Pagamento p) {
                return id.equals(p.getId());
            }
            return false;
        });
    }

    private static String cpfAleatorio() {
        return String.format("%011d", ThreadLocalRandom.current().nextLong(100_000_000_000L));
    }
}