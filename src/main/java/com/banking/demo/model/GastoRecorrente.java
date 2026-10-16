package com.banking.demo.model;

import java.util.List;

/**
 * Um gasto que se repete todo mês: assinaturas, contas fixas e afins.
 *
 * A detecção é por estabelecimento e por valor. `ASSINATURA` significa valor
 * estável (variabilidade baixa, típico de streaming, academia, plano de
 * celular); `FIXO` é o gasto que volta todo mês mas oscila, como mercado e
 * energia.
 */
public class GastoRecorrente {

    private String estabelecimento;
    private String categoria;

    /** `ASSINATURA` (valor estável) ou `FIXO` (oscila, mas se repete). */
    private String tipo;

    /** Média mensal do gasto. */
    private Double valorMedio;

    /** Em quantos meses distintos o gasto apareceu. */
    private Integer quantidadeMeses;

    /** Meses em que apareceu, em ordem (`AAAA-MM`). */
    private List<String> meses;

    private Double totalGasto;

    /**
     * Quanto o usuário economizaria por ano cancelando a assinatura.
     * Zero para `FIXO`, onde cancelar não é uma opção.
     */
    private Double economiaAnualPotencial;

    public String getEstabelecimento() { return estabelecimento; }
    public void setEstabelecimento(String estabelecimento) { this.estabelecimento = estabelecimento; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Double getValorMedio() { return valorMedio; }
    public void setValorMedio(Double valorMedio) { this.valorMedio = valorMedio; }

    public Integer getQuantidadeMeses() { return quantidadeMeses; }
    public void setQuantidadeMeses(Integer quantidadeMeses) { this.quantidadeMeses = quantidadeMeses; }

    public List<String> getMeses() { return meses; }
    public void setMeses(List<String> meses) { this.meses = meses; }

    public Double getTotalGasto() { return totalGasto; }
    public void setTotalGasto(Double totalGasto) { this.totalGasto = totalGasto; }

    public Double getEconomiaAnualPotencial() { return economiaAnualPotencial; }
    public void setEconomiaAnualPotencial(Double economiaAnualPotencial) { this.economiaAnualPotencial = economiaAnualPotencial; }
}
