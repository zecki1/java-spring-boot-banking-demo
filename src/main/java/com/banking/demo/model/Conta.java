package com.banking.demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model que representa uma Conta Bancária
 * Simula estrutura de conta em um core bancário
 */
public class Conta {

    private Long id;
    private String numeroConta;
    private String titular;
    private String cpf;
    private BigDecimal saldo;
    private BigDecimal saldoInicial;
    private String agencia;
    private LocalDateTime dataAbertura;
    private boolean ativa;

    // Construtor padrão
    public Conta() {
        this.ativa = true;
        this.dataAbertura = LocalDateTime.now();
    }

    // Construtor completo
    public Conta(Long id, String numeroConta, String titular, String cpf, 
                 BigDecimal saldoInicial, String agencia) {
        this();
        this.id = id;
        this.numeroConta = numeroConta;
        this.titular = titular;
        this.cpf = cpf;
        this.saldoInicial = saldoInicial;
        this.saldo = saldoInicial;
        this.agencia = agencia;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroConta() { return numeroConta; }
    public void setNumeroConta(String numeroConta) { this.numeroConta = numeroConta; }

    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }

    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(BigDecimal saldoInicial) { 
        this.saldoInicial = saldoInicial;
        if (this.saldo == null) {
            this.saldo = saldoInicial;
        }
    }

    public String getAgencia() { return agencia; }
    public void setAgencia(String agencia) { this.agencia = agencia; }

    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(LocalDateTime dataAbertura) { this.dataAbertura = dataAbertura; }

    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }

    @Override
    public String toString() {
        return "Conta{" +
                "id=" + id +
                ", numeroConta='" + numeroConta + '\'' +
                ", titular='" + titular + '\'' +
                ", saldo=" + saldo +
                ", agencia='" + agencia + '\'' +
                '}';
    }
}