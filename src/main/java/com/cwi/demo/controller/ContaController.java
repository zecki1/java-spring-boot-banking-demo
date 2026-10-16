package com.cwi.demo.controller;

import com.cwi.demo.model.ApiResponse;
import com.cwi.demo.model.Conta;
import com.cwi.demo.model.TransferenciaRequest;
import com.cwi.demo.service.ContaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Controller para operações bancárias
 * Simula endpoints de um core bancário (como Bradesco)
 * 
 * Endpoints:
 * - GET  /api/contas              - Listar todas as contas
 * - GET  /api/contas/{id}         - Buscar conta por ID
 * - POST /api/contas              - Criar nova conta
 * - POST /api/transferencias      - Realizar transferência entre contas
 * - GET  /api/contas/{id}/saldo   - Consultar saldo
 * 
 * Integração com API do Banco Central para dados reais:
 * - GET  /api/mercado/selic       - Taxa Selic atual
 * - GET  /api/mercado/cambio      - Câmbio USD/BRL
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ContaController {

    @Autowired
    private ContaService contaService;

    /**
     * GET /api/contas
     * Lista todas as contas cadastradas
     */
    @GetMapping("/contas")
    public ResponseEntity<ApiResponse<List<Conta>>> listarContas() {
        List<Conta> contas = contaService.listarTodas();
        return ResponseEntity.ok(ApiResponse.success("Contas encontradas", contas, contas.size()));
    }

    /**
     * GET /api/contas/{id}
     * Busca uma conta pelo ID
     */
    @GetMapping("/contas/{id}")
    public ResponseEntity<ApiResponse<Conta>> buscarConta(@PathVariable Long id) {
        Optional<Conta> conta = contaService.buscarPorId(id);
        if (conta.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success("Conta encontrada", conta.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Conta de ID " + id + " não encontrada"));
    }

    /**
     * GET /api/contas/{id}/saldo
     * Consulta o saldo de uma conta específica
     */
    @GetMapping("/contas/{id}/saldo")
    public ResponseEntity<ApiResponse<BigDecimal>> consultarSaldo(@PathVariable Long id) {
        Optional<BigDecimal> saldo = contaService.consultarSaldo(id);
        if (saldo.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success("Saldo da conta", saldo.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Conta de ID " + id + " não encontrada"));
    }

    /**
     * POST /api/contas
     * Cria uma nova conta bancária
     * Validações: titular obrigatório, saldo inicial >= 0
     */
    @PostMapping("/contas")
    public ResponseEntity<ApiResponse<Conta>> criarConta(@RequestBody Conta novaConta) {
        if (novaConta.getTitular() == null || novaConta.getTitular().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Titular é obrigatório"));
        }

        if (novaConta.getSaldoInicial() == null) {
            novaConta.setSaldoInicial(BigDecimal.ZERO);
        }

        if (novaConta.getSaldoInicial().compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Saldo inicial não pode ser negativo"));
        }

        Conta contaCriada = contaService.criar(novaConta);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conta criada com sucesso", contaCriada));
    }

    /**
     * POST /api/transferencias
     * Realiza transferência entre duas contas
     * 
     * Regras de negócio (como em um core bancário real):
     * - Conta origem e destino devem existir
     * - Conta origem deve ter saldo suficiente
     * - Não pode transferir para a mesma conta
     * - Valor deve ser maior que zero
     * 
     * Em produção, isso teria @Transactional para garantir atomicidade
     */
    @PostMapping("/transferencias")
    public ResponseEntity<ApiResponse<Conta>> realizarTransferencia(
            @RequestBody TransferenciaRequest request) {
        
        // Validações de negócio (como em um core bancário real)
        if (request.getContaOrigem() == null || request.getContaDestino() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Contas de origem e destino são obrigatórias"));
        }

        if (request.getContaOrigem().equals(request.getContaDestino())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Não é possível transferir para a mesma conta"));
        }

        if (request.getValor() == null || request.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Valor deve ser maior que zero"));
        }

        try {
            Optional<Conta> resultado = contaService.transferir(
                request.getContaOrigem(), 
                request.getContaDestino(), 
                request.getValor()
            );

            if (resultado.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("Transferência realizada com sucesso", resultado.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Conta não encontrada ou saldo insuficiente"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erro ao processar transferência: " + e.getMessage()));
        }
    }
}