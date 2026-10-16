package com.cwi.demo.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Wrapper class para respostas da API
 * Equivalente ao padrão { success: true, data: [...] } usado no Express
 * 
 * Diferença importante:
 * - Java usa Generics <T> para type-safety
 * - Node.js usa objetos JavaScript dinâmicos
 */
public class ApiResponse<T> {
    private boolean success;
    private String mensagem;
    private T data;
    private Integer count;
    private LocalDateTime timestamp;

    // Construtores
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(boolean success, T data) {
        this();
        this.success = success;
        this.data = data;
    }

    public ApiResponse(boolean success, String mensagem, T data) {
        this(success, data);
        this.mensagem = mensagem;
    }

    public ApiResponse(boolean success, String mensagem, T data, Integer count) {
        this(success, mensagem, data);
        this.count = count;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    // Métodos factory estáticos para facilitar uso
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data);
    }

    public static <T> ApiResponse<T> success(String mensagem, T data) {
        return new ApiResponse<>(true, mensagem, data);
    }

    public static <T> ApiResponse<T> success(String mensagem, T data, Integer count) {
        return new ApiResponse<>(true, mensagem, data, count);
    }

    public static <T> ApiResponse<T> error(String mensagem) {
        return new ApiResponse<>(false, mensagem, null);
    }
}