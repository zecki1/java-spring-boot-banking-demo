package com.cwi.demo.controller;

import com.cwi.demo.model.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Controller para dados de mercado financeiro
 * Integra com API real do Banco Central do Brasil
 * 
 * APIs do Banco Central (gratuitas, sem autenticação):
 * - https://api.bcb.gov.br/dados/serie/bcdata.sgs.11/dados/ultimos/1?formato=json  (Selic)
 * - https://economia.awesomeapi.com.br/last/USD-BRL  (Câmbio)
 * 
 * Isso demonstra:
 * - Integração com APIs externas
 * - Conhecimento do domínio bancário brasileiro
 * - Uso de RestTemplate/WebClient (Spring)
 */
@RestController
@RequestMapping("/api/mercado")
@CrossOrigin(origins = "*")
public class MarketController {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * GET /api/mercado/selic
     * Consulta a taxa Selic atual na API do Banco Central
     * 
     * Selic = taxa básica de juros do Brasil
     * É a taxa que define o custo do dinheiro na economia
     * Bancos usam para definir taxas de empréstimo, financiamento, etc
     */
    @GetMapping("/selic")
    public ResponseEntity<ApiResponse<Map>> consultarSelic() {
        try {
            // API do Banco Central - Série 11 = Taxa Selic acumulada no mês
            String url = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.11/dados/ultimos/1?formato=json";
            
            // Em produção, usar WebClient (reativo) em vez de RestTemplate (bloqueante)
            Map[] response = restTemplate.getForObject(url, Map[].class);
            
            if (response != null && response.length > 0) {
                Map selicData = response[0];
                return ResponseEntity.ok(ApiResponse.success("Taxa Selic obtida com sucesso", selicData));
            } else {
                return ResponseEntity.ok(ApiResponse.error("Não foi possível obter dados da Selic"));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Erro ao consultar API do Banco Central: " + e.getMessage()));
        }
    }

    /**
     * GET /api/mercado/cambio
     * Consulta câmbio USD/BRL
     * 
     * Usa a AwesomeAPI (gratuita) em vez do Banco Central
     * A AwesomeAPI é mais simples e não requer token
     */
    @GetMapping("/cambio")
    public ResponseEntity<ApiResponse<Map>> consultarCambio() {
        try {
            String url = "https://economia.awesomeapi.com.br/last/USD-BRL";
            
            Map response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("USDBRL")) {
                Map usdBrl = (Map) response.get("USDBRL");
                return ResponseEntity.ok(ApiResponse.success("Câmbio USD/BRL obtido com sucesso", usdBrl));
            } else {
                return ResponseEntity.ok(ApiResponse.error("Não foi possível obter dados de câmbio"));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Erro ao consultar API de câmbio: " + e.getMessage()));
        }
    }

    /**
     * GET /api/mercado/ipca
     * Consulta IPCA (índice de inflação)
     * Série 433 = IPCA - variação mensal
     */
    @GetMapping("/ipca")
    public ResponseEntity<ApiResponse<Map>> consultarIPCA() {
        try {
            String url = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.433/dados/ultimos/1?formato=json";
            
            Map[] response = restTemplate.getForObject(url, Map[].class);
            
            if (response != null && response.length > 0) {
                Map ipcaData = response[0];
                return ResponseEntity.ok(ApiResponse.success("IPCA obtido com sucesso", ipcaData));
            } else {
                return ResponseEntity.ok(ApiResponse.error("Não foi possível obter dados do IPCA"));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Erro ao consultar API do Banco Central: " + e.getMessage()));
        }
    }
}