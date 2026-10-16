package com.banking.demo.model;

/**
 * Resumo de entradas e saídas de um mês, com a variação contra o mês anterior.
 *
 * É o que a tela de "comparativo mensal" renderiza: se você gastou mais ou
 * menos que no mês anterior, e quanto.
 */
public class ComparativoMensal {

    /** Mês no formato `AAAA-MM`. */
    private String mes;

    private Double entradas;
    private Double saidas;
    private Double saldo;

    /** Variação das saídas vs. o mês anterior, em %; nulo no mês mais antigo. */
    private Double variacaoSaidasPercent;

    /** Variação das entradas vs. o mês anterior, em %; nulo no mês mais antigo. */
    private Double variacaoEntradasPercent;

    public String getMes() { return mes; }
    public void setMes(String mes) { this.mes = mes; }

    public Double getEntradas() { return entradas; }
    public void setEntradas(Double entradas) { this.entradas = entradas; }

    public Double getSaidas() { return saidas; }
    public void setSaidas(Double saidas) { this.saidas = saidas; }

    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }

    public Double getVariacaoSaidasPercent() { return variacaoSaidasPercent; }
    public void setVariacaoSaidasPercent(Double variacaoSaidasPercent) { this.variacaoSaidasPercent = variacaoSaidasPercent; }

    public Double getVariacaoEntradasPercent() { return variacaoEntradasPercent; }
    public void setVariacaoEntradasPercent(Double variacaoEntradasPercent) { this.variacaoEntradasPercent = variacaoEntradasPercent; }
}
