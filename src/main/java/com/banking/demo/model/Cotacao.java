package com.banking.demo.model;

import java.math.BigDecimal;

/**
 * Cotação de um ativo negociado na B3 (ação, ETF, FI, BDR...).
 *
 * Os nomes dos campos são os MESMOS usados pelo frontend Angular
 * (`angular-ledger-bank`, modelo `Quote`). Isso é proposital: o Java é o único
 * dono da integração externa, então ele normaliza a resposta do provedor de
 * mercado e entrega ao Angular já no formato que a tela espera. Sem duplicar
 * regra de mapeamento nos dois lados.
 *
 * Fonte atual: Yahoo Finance (`/v8/finance/chart/{simbolo}`), que responde sem
 * token e com CORS liberado do servidor. O mercado brasileiro usa o sufixo `.SA`
 * no Yahoo (ex.: PETR4 -> PETR4.SA), mas o `ticker` devolvido aqui mantém o
 * código curto que o usuário digitou.
 */
public class Cotacao {

    /** Código curto, sem sufixo de bolsa (ex.: `PETR4`). */
    private String ticker;

    /** Nome do ativo (ex.: `Petróleo Brasileiro S.A. - Petrobras`). */
    private String nome;

    /** Último preço de fechamento/mercado. */
    private BigDecimal preco;

    /** Variação absoluta em relação ao fechamento anterior. */
    private BigDecimal variacao;

    /** Variação percentual em relação ao fechamento anterior. */
    private BigDecimal variacaoPercent;

    /** Código ISO da moeda (ex.: `BRL`). */
    private String moeda;

    /** Preço de abertura do pregão corrente. */
    private BigDecimal abertura;

    /** Mínima do dia. */
    private BigDecimal minima;

    /** Máxima do dia. */
    private BigDecimal maxima;

    /** Volume negociado (ações). */
    private Long volume;

    /**
     * Valor de mercado. A API gratuita do Yahoo não expõe market cap pelo
     * endpoint de chart (o `/v7/finance/quote` exigiu crumb e respondeu 401),
     * então fica zerado e a tela omite o bloco quando o valor é 0.
     */
    private BigDecimal valorMercado;

    /** Menor preço dos últimos 52 semanas. */
    private BigDecimal minimo52;

    /** Maior preço dos últimos 52 semanas. */
    private BigDecimal maximo52;

    /** Instante da cotação em ISO-8601 (ex.: `2026-10-16T14:30:00Z`). */
    private String atualizadoEm;

    /** URL do logo quando o provedor devolve; vazio caso contrário. */
    private String logoUrl;

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    public BigDecimal getVariacao() { return variacao; }
    public void setVariacao(BigDecimal variacao) { this.variacao = variacao; }

    public BigDecimal getVariacaoPercent() { return variacaoPercent; }
    public void setVariacaoPercent(BigDecimal variacaoPercent) { this.variacaoPercent = variacaoPercent; }

    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }

    public BigDecimal getAbertura() { return abertura; }
    public void setAbertura(BigDecimal abertura) { this.abertura = abertura; }

    public BigDecimal getMinima() { return minima; }
    public void setMinima(BigDecimal minima) { this.minima = minima; }

    public BigDecimal getMaxima() { return maxima; }
    public void setMaxima(BigDecimal maxima) { this.maxima = maxima; }

    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }

    public BigDecimal getValorMercado() { return valorMercado; }
    public void setValorMercado(BigDecimal valorMercado) { this.valorMercado = valorMercado; }

    public BigDecimal getMinimo52() { return minimo52; }
    public void setMinimo52(BigDecimal minimo52) { this.minimo52 = minimo52; }

    public BigDecimal getMaximo52() { return maximo52; }
    public void setMaximo52(BigDecimal maximo52) { this.maximo52 = maximo52; }

    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
}
