package com.cwi.demo.controller;

import com.cwi.demo.model.ApiResponse;
import com.cwi.demo.model.Transacao;
import com.cwi.demo.service.TransacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller para endpoints de Transações
 * Equivalente às rotas definidas no server.js com app.get(), app.post(), etc.
 * 
 * Diferenças importantes:
 * - @RestController = @Controller + @ResponseBody (retorna JSON automaticamente)
 * - @Autowired injeta o Service (Dependency Injection)
 * - @GetMapping/@PostMapping/etc mapeiam HTTP methods para métodos Java
 * - ResponseEntity<T> permite controlar status code HTTP
 * - @PathVariable extrai valor da URL (como req.params.id no Express)
 * - @RequestBody converte JSON para objeto Java (Jackson)
 * 
 * Equivalências:
 * Express                          Spring Boot
 * -------------------------------- ---------------------------
 * app.get('/api', handler)        @GetMapping("/api")
 * req.query.page                  @RequestParam(required=false)
 * req.params.id                    @PathVariable("id")
 * req.body                        @RequestBody
 * res.json(data)                   return ResponseEntity.ok(data)
 * res.status(404).json({err})      return ResponseEntity.status(404).body(...)
 */
@RestController
@RequestMapping("/api/transacoes")
@CrossOrigin(origins = "*") // Similar a cors({ origin: '*' }) no Express
public class TransacaoController {

    @Autowired
    private TransacaoService transacaoService; // Dependency Injection (como require no Node)

    /**
     * GET /api/transacoes
     * Equivalente a app.get('/api/transacoes', (req, res) => {...})
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Transacao>>> listarTodas() {
        ApiResponse<List<Transacao>> response = transacaoService.listarTodas();
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/transacoes
     * Equivalente a app.post('/api/transacoes', (req, res) => {...})
     * @RequestBody converte o JSON recebido para um objeto Transacao
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Transacao>> criar(@RequestBody Transacao novaTransacao) {
        ApiResponse<Transacao> response = transacaoService.criar(novaTransacao);
        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * GET /api/transacoes/{id}
     * Equivalente a app.get('/api/transacoes/:id', (req, res) => {...})
     * @PathVariable("id") extrai o valor de :id na URL
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Transacao>> buscarPorId(@PathVariable Long id) {
        Optional<ApiResponse<Transacao>> response = transacaoService.buscarPorId(id);
        if (response.isPresent() && response.get().isSuccess()) {
            return ResponseEntity.ok(response.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                response.orElse(ApiResponse.error("Transação não encontrada"))
            );
        }
    }

    /**
     * PUT /api/transacoes/{id}
     * Equivalente a app.put('/api/transacoes/:id', (req, res) => {...})
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Transacao>> atualizar(
            @PathVariable Long id,
            @RequestBody Transacao transacaoAtualizada) {
        Optional<ApiResponse<Transacao>> response = transacaoService.atualizar(id, transacaoAtualizada);
        if (response.isPresent() && response.get().isSuccess()) {
            return ResponseEntity.ok(response.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                response.orElse(ApiResponse.error("Transação não encontrada"))
            );
        }
    }

    /**
     * DELETE /api/transacoes/{id}
     * Equivalente a app.delete('/api/transacoes/:id', (req, res) => {...})
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Transacao>> remover(@PathVariable Long id) {
        Optional<ApiResponse<Transacao>> response = transacaoService.remover(id);
        if (response.isPresent() && response.get().isSuccess()) {
            return ResponseEntity.ok(response.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                response.orElse(ApiResponse.error("Transação não encontrada"))
            );
        }
    }
}