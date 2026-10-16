package com.banking.demo.model;

import java.math.BigDecimal;

/**
 * Um ponto de uma série histórica de preços.
 *
 * Espelha o modelo `PricePoint` do frontend Angular (`{ date, close }`): a data
 * em `AAAA-MM-DD` e o preço de fechamento do período.
 */
public class PontoSerie {

    /** Data do pregão em `AAAA-MM-DD`. */
    private String date;

    /** Preço de fechamento do período. */
    private BigDecimal close;

    public PontoSerie() {
    }

    public PontoSerie(String date, BigDecimal close) {
        this.date = date;
        this.close = close;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }
}
