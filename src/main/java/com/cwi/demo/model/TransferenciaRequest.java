package com.cwi.demo.model;

import java.math.BigDecimal;

/**
 * Request DTO para transferência entre contas
 * Usado no endpoint POST /api/transferencias
 * 
 * DTO (Data Transfer Object):
 * - Separa o formato da requisição do model interno
 * - Permite validação específica para cada endpoint
 * - Boa prática em APIs REST
 */
public class TransferenciaRequest {

    private Long contaOrigem;
    private Long contaDestino;
    private BigDecimal valor;
    private String descricao;

    // Construtor padrão
    public TransferenciaRequest() {
    }

    // Construtor completo
    public TransferenciaRequest(Long contaOrigem, Long contaDestino, BigDecimal valor, String descricao) {
        this.contaOrigem = contaOrigem;
        this.contaDestino = contaDestino;
        this.valor = valor;
        this.descricao = descricao;
    }

    // Getters and Setters
    public Long getContaOrigem() { return contaOrigem; }
    public void setContaOrigem(Long contaOrigem) { this.contaOrigem = contaOrigem; }

    public Long getContaDestino() { return contaDestino; }
    public void setContaDestino(Long contaDestino) { this.contaDestino = contaDestino; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}