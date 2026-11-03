package com.locadora.menu;

import com.locadora.exception.LocacaoNaoEncontradaException;
import com.locadora.model.Locacao;
import com.locadora.model.Pagamento;
import com.locadora.repository.PagamentoRepository;
import com.locadora.service.LocacaoService;
import com.locadora.service.PagamentoService;

import java.util.Optional;
import java.util.Scanner;

public final class MenuPagamento extends MenuBase {

    private final LocacaoService locacoes;
    private final PagamentoService pagamentos;
    private final PagamentoRepository pagamentoRepository;

    public MenuPagamento(LocacaoService locacoes, PagamentoService pagamentos,
                         PagamentoRepository pagamentoRepository, Scanner entrada) {
        super(entrada);
        this.locacoes = locacoes;
        this.pagamentos = pagamentos;
        this.pagamentoRepository = pagamentoRepository;
    }

    public void executar() {
        while (true) {
            System.out.println("""

                    --- Pagamentos ---
                    1 - Gerar para locação devolvida
                    2 - Buscar por id
                    3 - Buscar por locação
                    4 - Listar todos
                    0 - Voltar
                    """);
            int opcao = lerInteiro("Opção: ");
            if (opcao == 0) {
                return;
            }
            try {
                switch (opcao) {
                    case 1 -> gerar();
                    case 2 -> buscarPorId();
                    case 3 -> buscarPorLocacao();
                    case 4 -> listar(pagamentos.listarTodos(), MenuPagamento::imprimir);
                    default -> System.out.println("  Opção inválida.");
                }
            } catch (RuntimeException e) {
                tratarErro(e);
            }
        }
    }

    private void gerar() {
        long locacaoId = lerLong("Id da locação: ");
        Locacao locacao = locacoes.buscarPorId(locacaoId)
                .orElseThrow(() -> new LocacaoNaoEncontradaException(locacaoId));
        Pagamento pagamento = pagamentos.gerar(locacaoId, locacao.getValorDiaria());
        System.out.println("  Pagamento gerado:");
        imprimir(pagamento);
    }

    private void buscarPorId() {
        long id = lerLong("Id: ");
        Optional<Pagamento> pagamento = pagamentos.buscarPorId(id);
        if (pagamento.isPresent()) {
            imprimir(pagamento.get());
        } else {
            System.out.println("  Pagamento não encontrado.");
        }
    }

    private void buscarPorLocacao() {
        long locacaoId = lerLong("Id da locação: ");
        pagamentoRepository.buscarPorLocacao(locacaoId).ifPresentOrElse(
                MenuPagamento::imprimir,
                () -> System.out.println("  Nenhum pagamento para essa locação."));
    }

    private static void imprimir(Pagamento pagamento) {
        System.out.printf("  id=%d  locação=%d  valor=R$ %s  data=%s  código=%s%n",
                pagamento.getId(), pagamento.getLocacaoId(), pagamento.getValor(),
                pagamento.getDataPagamento(),
                pagamento.getCodigoPagamento().getCodigoGerado());
    }
}
