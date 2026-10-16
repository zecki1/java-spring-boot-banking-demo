package com.banking.demo.model;

/**
 * Uma sugestão de economia gerada a partir do próprio extrato do usuário.
 *
 * As regras atuais são:
 * - `RENOVIACAO` — assinatura de valor estável que vem sendo paga todo mês;
 * - `CRESCIMENTO` — mês em que as saídas subiram forte em relação ao anterior.
 *
 * É uma dica calculada a partir dos dados, não recomendação financeira.
 */
public class SugestaoEconomia {

    /** `RENOVIACAO`, `CRESCIMENTO` (novas regras podem entrar aqui). */
    private String tipo;

    private String titulo;

    /** Explica o porquê, com os números que sustentam a sugestão. */
    private String descricao;

    /** Quanto representaria por mês, em reais. */
    private Double impactoMensal;

    /** `ALTA`, `MEDIA` ou `BAIXA` — ordena a tela. */
    private String prioridade;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Double getImpactoMensal() { return impactoMensal; }
    public void setImpactoMensal(Double impactoMensal) { this.impactoMensal = impactoMensal; }

    public String getPrioridade() { return prioridade; }
    public void setPrioridade(String prioridade) { this.prioridade = prioridade; }
}
