package com.banking.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model class para representar uma Transação
 * Equivalente a uma interface TypeScript ou objeto JavaScript no Node.js
 * 
 * Diferenças importantes:
 * - Java é fortemente tipado (precisa declarar tipo de cada campo)
 * - Usa encapsulamento (private + getters/setters)
 * - Tem construtor explícito
 * - Suporta Bean Validation (anotações @NotBlank, @NotNull, etc)
 */
public class Transacao {

    private Long id;
    private String descricao;
    private Double valor;
    private LocalDate data;
    private String tipo;
    private LocalDateTime createdAt;

    // Construtor padrão (necessário para frameworks como Jackson, Spring, JPA)
    public Transacao() {
    }

    // Construtor completo
    public Transacao(Long id, String descricao, Double valor, LocalDate data, String tipo) {
        this.id = id;
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
        this.tipo = tipo;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Método toString para facilitar logging/debug
    @Override
    public String toString() {
        return "Transacao{" +
                "id=" + id +
                ", descricao='" + descricao + '\'' +
                ", valor=" + valor +
                ", data=" + data +
                ", tipo='" + tipo + '\'' +
                '}';
    }
}