package com.locadora.menu;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

public abstract class MenuBase {

    protected final Scanner entrada;

    protected MenuBase(Scanner entrada) {
        this.entrada = entrada;
    }

    protected String ler(String rotulo) {
        System.out.print(rotulo);
        if (!entrada.hasNextLine()) {
            throw new FimDaEntradaException();
        }
        return entrada.nextLine().trim();
    }

    protected int lerInteiro(String rotulo) {
        while (true) {
            String valor = ler(rotulo);
            if (valor.isEmpty()) {
                System.out.println("  Informe um número inteiro.");
                continue;
            }
            try {
                return Integer.parseInt(valor);
            } catch (NumberFormatException e) {
                System.out.println("  Número inválido.");
            }
        }
    }

    protected long lerLong(String rotulo) {
        while (true) {
            String valor = ler(rotulo);
            if (valor.isEmpty()) {
                System.out.println("  Informe um número inteiro.");
                continue;
            }
            try {
                return Long.parseLong(valor);
            } catch (NumberFormatException e) {
                System.out.println("  Número inválido.");
            }
        }
    }

    protected int lerInteiroComPadrao(String rotulo, int padrao) {
        while (true) {
            String valor = ler(rotulo + " [" + padrao + "]: ");
            if (valor.isEmpty()) {
                return padrao;
            }
            try {
                return Integer.parseInt(valor);
            } catch (NumberFormatException e) {
                System.out.println("  Número inválido.");
            }
        }
    }

    protected long lerLongComPadrao(String rotulo, long padrao) {
        while (true) {
            String valor = ler(rotulo + " [" + padrao + "]: ");
            if (valor.isEmpty()) {
                return padrao;
            }
            try {
                return Long.parseLong(valor);
            } catch (NumberFormatException e) {
                System.out.println("  Número inválido.");
            }
        }
    }

    protected BigDecimal lerValor(String rotulo) {
        while (true) {
            String valor = ler(rotulo).replace(',', '.');
            if (valor.isEmpty()) {
                System.out.println("  Informe o valor.");
                continue;
            }
            try {
                return new BigDecimal(valor);
            } catch (NumberFormatException e) {
                System.out.println("  Valor inválido. Ex.: 8,50");
            }
        }
    }

    protected BigDecimal lerValorComPadrao(String rotulo, BigDecimal padrao) {
        while (true) {
            String valor = ler(rotulo + " [" + padrao + "]: ").replace(',', '.');
            if (valor.isEmpty()) {
                return padrao;
            }
            try {
                return new BigDecimal(valor);
            } catch (NumberFormatException e) {
                System.out.println("  Valor inválido. Ex.: 8,50");
            }
        }
    }

    protected LocalDate lerDataOpcional(String rotulo) {
        while (true) {
            String valor = ler(rotulo);
            if (valor.isEmpty()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(valor);
            } catch (DateTimeParseException e) {
                System.out.println("  Data inválida. Use yyyy-MM-dd (ex.: 2026-11-02).");
            }
        }
    }

    protected String lerComPadrao(String rotulo, String padrao) {
        String valor = ler(rotulo + " [" + padrao + "]: ");
        return valor.isEmpty() ? padrao : valor;
    }

    protected static <T> void listar(List<T> itens, Consumer<T> printer) {
        if (itens.isEmpty()) {
            System.out.println("  Nenhum registro encontrado.");
            return;
        }
        itens.forEach(printer);
    }

    protected static void tratarErro(RuntimeException e) {
        if (e instanceof FimDaEntradaException) {
            throw e;
        }
        System.out.println("  ERRO: " + mensagem(e));
    }

    protected static String mensagem(Throwable e) {
        String mensagem = e.getMessage();
        return mensagem == null || mensagem.isBlank() ? e.getClass().getSimpleName() : mensagem;
    }
}
