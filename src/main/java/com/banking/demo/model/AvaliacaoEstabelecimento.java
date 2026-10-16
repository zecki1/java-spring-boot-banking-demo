package com.banking.demo.model;

/**
 * Avaliação média de um estabelecimento, a partir das notas dadas nas
 * transações daquele lojista.
 *
 * Serve para a tela "avaliar lojas": o usuário vê quanto já gastou, quantas
 * vezes avaliou e o que comentou da última vez.
 */
public class AvaliacaoEstabelecimento {

    private String estabelecimento;

    /** Média das notas de 1 a 5; zero quando ninguém avaliou ainda. */
    private Double notaMedia;

    /** Quantas transações receberam nota. */
    private Integer totalAvaliacoes;

    /** Quantas transações daquele estabelecimento existem no total. */
    private Integer totalTransacoes;

    /** Comentário da avaliação mais recente. */
    private String ultimoComentario;

    public String getEstabelecimento() { return estabelecimento; }
    public void setEstabelecimento(String estabelecimento) { this.estabelecimento = estabelecimento; }

    public Double getNotaMedia() { return notaMedia; }
    public void setNotaMedia(Double notaMedia) { this.notaMedia = notaMedia; }

    public Integer getTotalAvaliacoes() { return totalAvaliacoes; }
    public void setTotalAvaliacoes(Integer totalAvaliacoes) { this.totalAvaliacoes = totalAvaliacoes; }

    public Integer getTotalTransacoes() { return totalTransacoes; }
    public void setTotalTransacoes(Integer totalTransacoes) { this.totalTransacoes = totalTransacoes; }

    public String getUltimoComentario() { return ultimoComentario; }
    public void setUltimoComentario(String ultimoComentario) { this.ultimoComentario = ultimoComentario; }
}
