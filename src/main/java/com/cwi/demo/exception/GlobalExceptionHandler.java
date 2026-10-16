package com.cwi.demo.exception;

import com.cwi.demo.model.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;

/**
 * Global Exception Handler
 * Equivalente ao handler de erro no Express:
 * 
 * app.use((err, req, res, next) => {
 *   res.status(500).json({ success: false, mensagem: 'Erro interno' });
 * });
 * 
 * @ControllerAdvice = escaneia todos os controllers e aplica handlers globais
 * @ExceptionHandler(Exception.class) = captura qualquer exceção não tratada
 * 
 * Diferença importante:
 * - Em Express: tratamento via callback (err, req, res, next)
 * - Em Spring: tratamento via anotações e exception classes específicas
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handler para exceções genéricas (erros inesperados)
     * Equivalente a app.use((err, ...) => res.status(500).json(...))
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        ApiResponse<Object> response = ApiResponse.error("Erro interno do servidor");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handler para IllegalArgumentException (validações de negócio)
     * Podemos criar handlers específicos para cada tipo de exceção
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}