package com.banking.demo.model;

import java.util.List;

/**
 * Análise de gastos do extrato: comparativo mensal, gastos recorrentes e
 * sugestões de economia.
 *
 * Um único endpoint devolve tudo porque a tela de análise é lida de uma vez e
 * os três blocos saem dos mesmos dados — separar em três requisições só
 * duplicaria trabalho no servidor.
 */
public class AnaliseGastos {

    /** Mês mais recente com movimentação, em `AAAA-MM`. */
    private String mesAtual;

    private Double entradasMesAtual;
    private Double saidasMesAtual;
    private Double saldoMesAtual;

    /** Do mês mais recente para o mais antigo. */
    private List<ComparativoMensal> comparativo;

    /** Gastos que se repetem todo mês, do maior para o menor. */
    private List<GastoRecorrente> recorrentes;

    /** Dicas geradas a partir dos dados, da mais impactante para a menos. */
    private List<SugestaoEconomia> sugestoes;

    /** Total de transações analisadas (mostra a base da análise). */
    private Integer transacoesAnalisadas;

    /** Quantas transações já receberam nota do usuário. */
    private Integer transacoesAvaliadas;

    public String getMesAtual() { return mesAtual; }
    public void setMesAtual(String mesAtual) { this.mesAtual = mesAtual; }

    public Double getEntradasMesAtual() { return entradasMesAtual; }
    public void setEntradasMesAtual(Double entradasMesAtual) { this.entradasMesAtual = entradasMesAtual; }

    public Double getSaidasMesAtual() { return saidasMesAtual; }
    public void setSaidasMesAtual(Double saidasMesAtual) { this.saidasMesAtual = saidasMesAtual; }

    public Double getSaldoMesAtual() { return saldoMesAtual; }
    public void setSaldoMesAtual(Double saldoMesAtual) { this.saldoMesAtual = saldoMesAtual; }

    public List<ComparativoMensal> getComparativo() { return comparativo; }
    public void setComparativo(List<ComparativoMensal> comparativo) { this.comparativo = comparativo; }

    public List<GastoRecorrente> getRecorrentes() { return recorrentes; }
    public void setRecorrentes(List<GastoRecorrente> recorrentes) { this.recorrentes = recorrentes; }

    public List<SugestaoEconomia> getSugestoes() { return sugestoes; }
    public void setSugestoes(List<SugestaoEconomia> sugestoes) { this.sugestoes = sugestoes; }

    public Integer getTransacoesAnalisadas() { return transacoesAnalisadas; }
    public void setTransacoesAnalisadas(Integer transacoesAnalisadas) { this.transacoesAnalisadas = transacoesAnalisadas; }

    public Integer getTransacoesAvaliadas() { return transacoesAvaliadas; }
    public void setTransacoesAvaliadas(Integer transacoesAvaliadas) { this.transacoesAvaliadas = transacoesAvaliadas; }
}
