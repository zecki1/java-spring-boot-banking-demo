package com.banking.demo.model;

import java.util.List;

/**
 * Série histórica de um ativo, pronta para o gráfico/sparkline do Angular.
 *
 * Guardamos também o intervalo pedido porque a tela exibe de onde veio o dado
 * (mesma ideia do "origin" que já existe no frontend para a cotação ao vivo).
 */
public class SerieCotacao {

    /** Código curto do ativo, sem sufixo de bolsa (ex.: `PETR4`). */
    private String ticker;

    /** Código ISO da moeda (ex.: `BRL`). */
    private String moeda;

    /** Intervalo solicitado (ex.: `1mo`, `1y`). */
    private String range;

    /** Granularidade de cada ponto (ex.: `1d`, `1wk`). */
    private String interval;

    /** Pontos em ordem cronológica crescente. */
    private List<PontoSerie> pontos;

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }

    public String getRange() { return range; }
    public void setRange(String range) { this.range = range; }

    public String getInterval() { return interval; }
    public void setInterval(String interval) { this.interval = interval; }

    public List<PontoSerie> getPontos() { return pontos; }
    public void setPontos(List<PontoSerie> pontos) { this.pontos = pontos; }
}
