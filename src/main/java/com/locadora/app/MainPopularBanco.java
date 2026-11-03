package com.locadora.app;

import com.locadora.exception.CpfJaCadastradoException;
import com.locadora.exception.FilmeIndisponivelException;
import com.locadora.exception.LocacaoJaDevolvidaException;
import com.locadora.exception.LocacaoNaoDevolvidaException;
import com.locadora.exception.TituloJaExistenteException;
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
import com.locadora.util.Cpf;
import com.locadora.util.Keys;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Popula o Garnet com um cenário completo e deixa os dados gravados:
 * 3 clientes, 3 filmes, 2 locações devolvidas com pagamento e 1 locação em aberto.
 *
 * Diferente de {@link MainLocadora}, não remove nada no final. Pode ser executado
 * várias vezes: os CPFs são aleatórios e os títulos recebem um sufixo único.
 */
public final class MainPopularBanco {

    private static int verificacoes = 0;
    private static int falhas = 0;

    private MainPopularBanco() {
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

            String sufixo = UUID.randomUUID().toString().substring(0, 6);
            LocalDate hoje = LocalDate.now();

            passo(1, "cadastrar 3 clientes");
            Cliente ana = clientes.cadastrar("Ana Souza", cpfAleatorio());
            Cliente bruno = clientes.cadastrar("Bruno Lima", cpfAleatorio());
            Cliente carla = clientes.cadastrar("Carla Dias", cpfAleatorio());
            imprimir(ana);
            imprimir(bruno);
            imprimir(carla);
            conferir("busca por CPF com máscara encontra a Ana",
                    clientes.buscarPorCpf(Cpf.formatar(ana.getCpf()))
                            .map(ana::equals).orElse(false));
            esperarErro("CPF repetido é recusado", CpfJaCadastradoException.class,
                    () -> clientes.cadastrar("Ana Clone", ana.getCpf()));

            passo(2, "cadastrar 3 filmes");
            Filme cidade = filmes.cadastrar("Cidade de Deus " + sufixo, "Drama",
                    Year.of(2002), Duration.ofMinutes(130), new BigDecimal("8.50"));
            Filme bacurau = filmes.cadastrar("Bacurau " + sufixo, "Suspense",
                    Year.of(2019), Duration.ofMinutes(132), new BigDecimal("9.90"));
            Filme auto = filmes.cadastrar("O Auto da Compadecida " + sufixo, "Comédia",
                    Year.of(2000), Duration.ofMinutes(104), new BigDecimal("7.00"));
            imprimir(cidade);
            imprimir(bacurau);
            imprimir(auto);
            conferir("busca por título ignora maiúsculas",
                    filmes.buscarPorTitulo(cidade.getTitulo().toUpperCase())
                            .map(cidade::equals).orElse(false));
            conferir("os 3 filmes começam disponíveis",
                    disponivel(jedis, cidade) && disponivel(jedis, bacurau) && disponivel(jedis, auto));
            esperarErro("título repetido é recusado", TituloJaExistenteException.class,
                    () -> filmes.cadastrar(cidade.getTitulo().toLowerCase(), "Drama",
                            Year.of(2002), Duration.ofMinutes(130), new BigDecimal("8.50")));

            passo(3, "atualizar um cliente e um filme");
            Cliente brunoAtualizado =
                    clientes.atualizar(bruno.getId(), "Bruno Lima Santos", bruno.getCpf());
            Filme bacurauAtualizado = filmes.atualizar(bacurau.getId(), bacurau.getTitulo(),
                    bacurau.getGenero(), bacurau.getAno(), bacurau.getDuracao(),
                    new BigDecimal("11.00"));
            imprimir(brunoAtualizado);
            imprimir(bacurauAtualizado);
            conferir("nome do Bruno foi atualizado",
                    clientes.buscarPorId(bruno.getId())
                            .map(c -> "Bruno Lima Santos".equals(c.getNome())).orElse(false));
            conferir("diária do Bacurau foi atualizada para 11.00",
                    filmes.buscarPorId(bacurau.getId())
                            .map(f -> new BigDecimal("11.00").compareTo(f.getValorDiario()) == 0)
                            .orElse(false));

            passo(4, "remover um registro temporário");
            Filme temporario = filmes.cadastrar("Filme Temporário " + sufixo, "Teste",
                    Year.of(2024), Duration.ofMinutes(10), BigDecimal.ONE);
            conferir("remoção devolve true", filmes.remover(temporario.getId()));
            conferir("registro removido não é mais encontrado",
                    filmes.buscarPorId(temporario.getId()).isEmpty());
            conferir("índice de título do filme removido também sumiu",
                    jedis.get(Keys.idxFilmeTitulo(temporario.getTitulo())) == null);

            passo(5, "alugar filmes");
            Locacao locAna = locacoes.alugar(ana.getId(), cidade.getId());
            Locacao locBruno = locacoes.alugar(bruno.getId(), bacurau.getId());
            Locacao locCarla = locacoes.alugar(carla.getId(), auto.getId());
            imprimir(locAna);
            imprimir(locBruno);
            imprimir(locCarla);
            conferir("Cidade de Deus ficou indisponível", !disponivel(jedis, cidade));
            conferir("Bacurau ficou indisponível", !disponivel(jedis, bacurau));
            conferir("O Auto da Compadecida ficou indisponível", !disponivel(jedis, auto));
            conferir("as 3 locações estão abertas",
                    contemId(locacoes.listarAbertas(), locAna.getId())
                            && contemId(locacoes.listarAbertas(), locBruno.getId())
                            && contemId(locacoes.listarAbertas(), locCarla.getId()));
            esperarErro("alugar filme já alugado é recusado", FilmeIndisponivelException.class,
                    () -> locacoes.alugar(bruno.getId(), auto.getId()));
            esperarErro("pagar locação ainda aberta é recusado", LocacaoNaoDevolvidaException.class,
                    () -> pagamentos.gerar(locCarla.getId(), auto.getValorDiario()));

            passo(6, "devolver duas locações");
            Locacao devAna = locacoes.devolver(locAna.getId(), hoje.plusDays(3));
            Locacao devBruno = locacoes.devolver(locBruno.getId(), hoje.plusDays(1));
            imprimir(devAna);
            imprimir(devBruno);
            conferir("Cidade de Deus voltou a ficar disponível", disponivel(jedis, cidade));
            conferir("Bacurau voltou a ficar disponível", disponivel(jedis, bacurau));
            conferir("O Auto da Compadecida continua indisponível", !disponivel(jedis, auto));
            conferir("listarAbertas contém a locação da Carla e não as devolvidas",
                    contemId(locacoes.listarAbertas(), locCarla.getId())
                            && !contemId(locacoes.listarAbertas(), locAna.getId())
                            && !contemId(locacoes.listarAbertas(), locBruno.getId()));
            conferir("listarDevolvidas contém as locações da Ana e do Bruno",
                    contemId(locacoes.listarDevolvidas(), locAna.getId())
                            && contemId(locacoes.listarDevolvidas(), locBruno.getId()));
            conferir("listarPorCliente da Ana contém a locação dela",
                    contemId(locacoes.listarPorCliente(ana.getId()), locAna.getId()));
            conferir("listarPorFilme do Auto contém a locação da Carla",
                    contemId(locacoes.listarPorFilme(auto.getId()), locCarla.getId()));
            esperarErro("devolver duas vezes é recusado", LocacaoJaDevolvidaException.class,
                    () -> locacoes.devolver(locAna.getId()));

            passo(7, "gerar e reler pagamentos");
            Pagamento pagAna = pagamentos.gerar(locAna.getId(), cidade.getValorDiario());
            Pagamento pagBruno = pagamentos.gerar(locBruno.getId(), bacurauAtualizado.getValorDiario());
            imprimir(pagAna);
            imprimir(pagBruno);
            conferir("3 diárias de 8.50 = 25.50",
                    new BigDecimal("25.50").compareTo(pagAna.getValor()) == 0);
            conferir("1 diária de 11.00 = 11.00",
                    new BigDecimal("11.00").compareTo(pagBruno.getValor()) == 0);
            conferir("buscarPorLocacao devolve o pagamento da Ana",
                    pagamentoRepo.buscarPorLocacao(locAna.getId())
                            .map(pagAna::equals).orElse(false));
            conferir("os dois pagamentos foram gravados no Garnet",
                    pagamentoRepo.buscarPorId(pagAna.getId()).isPresent()
                            && pagamentoRepo.buscarPorId(pagBruno.getId()).isPresent());

            passo(8, "resumo do que ficou gravado no Garnet");
            System.out.printf("  clientes: %d%n", clientes.listar().size());
            System.out.printf("  filmes: %d%n", filmes.listarTodos().size());
            System.out.printf("  locações: %d (abertas: %d, devolvidas: %d)%n",
                    locacoes.listarTodos().size(),
                    locacoes.listarAbertas().size(),
                    locacoes.listarDevolvidas().size());
            System.out.printf("  pagamentos: %d%n", pagamentos.listarTodos().size());
            System.out.printf("  total de chaves no banco: %d%n", jedis.dbSize());
            System.out.println();
            mostrarChave(jedis, Keys.cliente(ana.getId()));
            mostrarChave(jedis, Keys.filme(auto.getId()));
            mostrarChave(jedis, Keys.locacao(locCarla.getId()));
            mostrarChave(jedis, Keys.pagamento(pagAna.getId()));
            System.out.println("  " + Keys.idxFilmeDisponivel() + " = "
                    + jedis.smembers(Keys.idxFilmeDisponivel()));
            System.out.println("  " + Keys.idxLocacaoAberta() + " = "
                    + jedis.smembers(Keys.idxLocacaoAberta()));
            System.out.printf("%nPronto: os dados ficaram no banco. "
                    + "Atualize a conexão na extensão para vê-los.%n");
        } finally {
            System.out.printf("%n== %d verificações, %d com ERRO ==%n", verificacoes, falhas);
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

    private static boolean disponivel(Jedis jedis, Filme filme) {
        return jedis.sismember(Keys.idxFilmeDisponivel(), String.valueOf(filme.getId()));
    }

    private static boolean contemId(List<Locacao> locacoes, Long id) {
        return locacoes.stream().anyMatch(locacao -> id.equals(locacao.getId()));
    }

    private static void mostrarChave(Jedis jedis, String chave) {
        System.out.println("  " + chave + " = " + jedis.get(chave));
    }

    private static void imprimir(Cliente cliente) {
        System.out.printf("  id=%d  nome=%s  cpf=%s%n",
                cliente.getId(), cliente.getNome(), Cpf.formatar(cliente.getCpf()));
    }

    private static void imprimir(Filme filme) {
        System.out.printf("  id=%d  %s (%s, %s)  %d min  R$ %s/dia%n",
                filme.getId(), filme.getTitulo(), filme.getGenero(), filme.getAno(),
                filme.getDuracao().toMinutes(), filme.getValorDiario());
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

    private static void imprimir(Pagamento pagamento) {
        System.out.printf("  id=%d  locação=%d  valor=R$ %s  data=%s  código=%s%n",
                pagamento.getId(), pagamento.getLocacaoId(), pagamento.getValor(),
                pagamento.getDataPagamento(),
                pagamento.getCodigoPagamento().getCodigoGerado());
    }

    private static String cpfAleatorio() {
        return String.format("%011d", ThreadLocalRandom.current().nextLong(100_000_000_000L));
    }
}
