package com.locadora.util;

import com.locadora.exception.CampoObrigatorioException;
import com.locadora.exception.DataInvalidaException;
import com.locadora.exception.LocacaoInvalidaException;
import com.locadora.exception.ValorInvalidoException;
import com.locadora.model.Locacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ValidadorLocacao {

    private ValidadorLocacao() {
    }

    public static Long validarClienteId(Long clienteId) {
        return validarIdObrigatorio(clienteId, "Cliente");
    }

    public static Long validarFilmeId(Long filmeId) {
        return validarIdObrigatorio(filmeId, "Filme");
    }

    public static BigDecimal validarValorDiaria(BigDecimal valorDiaria) {
        if (valorDiaria == null || valorDiaria.signum() < 0) {
            throw new ValorInvalidoException("Valor da diária inválido");
        }
        return valorDiaria;
    }

    public static LocalDate validarDataLocacao(LocalDate dataLocacao) {
        if (dataLocacao == null) {
            throw new CampoObrigatorioException("Data da locação");
        }
        return dataLocacao;
    }

    public static void validarDatas(LocalDate dataLocacao, LocalDate dataDevolucao) {
        validarDataLocacao(dataLocacao);

        if (dataDevolucao != null && dataDevolucao.isBefore(dataLocacao)) {
            throw new DataInvalidaException(
                    "Data de devolução não pode ser anterior à data da locação");
        }
    }

    public static void validar(Locacao locacao) {
        if (locacao == null) {
            throw new LocacaoInvalidaException("Locação é obrigatória");
        }

        validarClienteId(locacao.getClienteId());
        validarFilmeId(locacao.getFilmeId());
        validarValorDiaria(locacao.getValorDiaria());
        validarDatas(locacao.getDataLocacao(), locacao.getDataDevolucao());
    }

    public static void validarNova(Locacao locacao) {
        if (locacao == null) {
            throw new LocacaoInvalidaException("Locação é obrigatória");
        }
        if (locacao.getDataDevolucao() != null) {
            throw new LocacaoInvalidaException(
                    "Locação nova não pode ter data de devolução");
        }
    }

    public static void validarDevolucao(Locacao locacao) {
        if (locacao == null) {
            throw new LocacaoInvalidaException("Locação é obrigatória");
        }
        if (locacao.getDataDevolucao() == null) {
            throw new CampoObrigatorioException("Data de devolução");
        }

        validarDatas(locacao.getDataLocacao(), locacao.getDataDevolucao());
    }

    private static Long validarIdObrigatorio(Long id, String nomeCampo) {
        if (id == null || id <= 0) {
            throw new CampoObrigatorioException(nomeCampo);
        }
        return id;
    }
}
