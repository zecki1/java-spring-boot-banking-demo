package com.banking.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Uma transação do extrato.
 *
 * Além do valor e da data, o modelo carrega o que a análise de gastos precisa:
 * o estabelecimento (para achar recorrência), o método de pagamento (PIX,
 * débito, crédito...), o status (para notificar erro/estorno) e uma avaliação
 * do usuário — que também alimenta a nota média de cada loja.
 */
public class Transacao {

    private Long id;

    /** Descrição livre, editável pelo usuário depois de criada. */
    private String descricao;

    /** Quem recebeu/pagou: ex. `NETFLIX`, `UBER`, `POSTO IPIRANGA`. */
    private String estabelecimento;

    /** Categoria para agrupar os gastos: ex. `assinaturas`, `transporte`. */
    private String categoria;

    /** Forma de pagamento: `PIX`, `DEBITO`, `CREDITO`, `TED`, `BOLETO`, `DINHEIRO`. */
    private String metodo;

    /** `entrada` ou `saida` (mantido do modelo original). */
    private String tipo;

    /** `CONCLUIDA`, `PENDENTE`, `ERRO` ou `ESTORNADA`. */
    private String status;

    /** Valor positivo; o sinal vem de `tipo`. */
    private Double valor;

    private LocalDate data;

    /** Nota de 1 a 5 dada pelo usuário; `null` quando ainda não avaliou. */
    private Integer avaliacao;

    /** Comentário livre junto com a nota. */
    private String comentarioAvaliacao;

    /** Mensagem exibida na notificação quando `status` é `ERRO` ou `ESTORNADA`. */
    private String mensagemErro;

    /** De onde veio: `MANUAL`, `IMPORTACAO` (extrato OFX/CSV) ou `SIMULACAO`. */
    private String origem;

    private LocalDateTime createdAt;
    private LocalDateTime atualizadoEm;

    public Transacao() {
        this.status = "CONCLUIDA";
        this.origem = "MANUAL";
        this.createdAt = LocalDateTime.now();
    }

    /** Mantido para compatibilidade com o modelo original. */
    public Transacao(Long id, String descricao, Double valor, LocalDate data, String tipo) {
        this();
        this.id = id;
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
        this.tipo = tipo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getEstabelecimento() { return estabelecimento; }
    public void setEstabelecimento(String estabelecimento) { this.estabelecimento = estabelecimento; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public Integer getAvaliacao() { return avaliacao; }
    public void setAvaliacao(Integer avaliacao) { this.avaliacao = avaliacao; }

    public String getComentarioAvaliacao() { return comentarioAvaliacao; }
    public void setComentarioAvaliacao(String comentarioAvaliacao) { this.comentarioAvaliacao = comentarioAvaliacao; }

    public String getMensagemErro() { return mensagemErro; }
    public void setMensagemErro(String mensagemErro) { this.mensagemErro = mensagemErro; }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    @Override
    public String toString() {
        return "Transacao{" +
                "id=" + id +
                ", descricao='" + descricao + '\'' +
                ", estabelecimento='" + estabelecimento + '\'' +
                ", metodo='" + metodo + '\'' +
                ", tipo='" + tipo + '\'' +
                ", status='" + status + '\'' +
                ", valor=" + valor +
                ", data=" + data +
                '}';
    }
}
