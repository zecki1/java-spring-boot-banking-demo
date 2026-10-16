package com.banking.demo.controller;

import com.banking.demo.model.ApiResponse;
import com.banking.demo.model.Cotacao;
import com.banking.demo.model.SerieCotacao;
import com.banking.demo.service.CotacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Controller para dados de mercado financeiro
 * Integra com API real do Banco Central do Brasil
 * 
 * APIs do Banco Central (gratuitas, sem autenticação):
 * - https://api.bcb.gov.br/dados/serie/bcdata.sgs.11/dados/ultimos/1?formato=json  (Selic)
 * - https://economia.awesomeapi.com.br/last/USD-BRL  (Câmbio)
 * 
 * Integrações externas usadas aqui:
 * - Banco Central do Brasil / AwesomeAPI: Selic, câmbio e IPCA (gratuitas, sem token)
 * - Yahoo Finance: cotações da B3 e séries históricas (sem token, via {@link CotacaoService})
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

    /** Cotações da B3. Só este serviço conversa com o provedor de mercado. */
    private final CotacaoService cotacaoService;

    public MarketController(CotacaoService cotacaoService) {
        this.cotacaoService = cotacaoService;
    }

    /**
     * GET /api/mercado/cotacoes?tickers=PETR4,VALE3
     *
     * Endpoint usado pelo frontend Angular — o navegador fala só com o Java,
     * nunca direto com o provedor de mercado (que não libera CORS).
     *
     * Um ticker inexistente não derruba o lote: a resposta traz os que
     * responderam. Só falha quando nenhum ativo é encontrado.
     */
    @GetMapping("/cotacoes")
    public ResponseEntity<ApiResponse<List<Cotacao>>> cotacoes(
            @RequestParam(name = "tickers", required = false) String tickers) {

        String entrada = tickers == null ? "" : tickers;

        List<String> lista = Arrays.stream(entrada.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .toList();

        if (lista.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Informe ao menos um código, ex.: /api/mercado/cotacoes?tickers=PETR4,VALE3"));
        }

        try {
            List<Cotacao> cotacoes = cotacaoService.cotacoes(lista);
            return ResponseEntity.ok(
                    ApiResponse.success("Cotações obtidas com sucesso", cotacoes, cotacoes.size()));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(mensagemAmigavel(e)));
        }
    }

    /**
     * GET /api/mercado/series?ticker=PETR4&range=1mo&interval=1d
     *
     * Série histórica para o gráfico/sparkline do Angular. `range` aceita os
     * períodos do provedor (1d, 5d, 1mo, 3mo, 6mo, 1y, 2y, 5y, 10y, ytd, max).
     */
    @GetMapping("/series")
    public ResponseEntity<ApiResponse<SerieCotacao>> serie(
            @RequestParam String ticker,
            @RequestParam(defaultValue = "1mo") String range,
            @RequestParam(defaultValue = "1d") String interval) {

        if (ticker == null || ticker.isBlank()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Informe o código, ex.: /api/mercado/series?ticker=PETR4"));
        }

        // Defaults também no código (e não só no @RequestParam) para o
        // comportamento ficar explícito e testável sem subir o MockMvc.
        String periodo = range == null || range.isBlank() ? "1mo" : range.trim();
        String granularidade = interval == null || interval.isBlank() ? "1d" : interval.trim();

        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Série obtida com sucesso", cotacaoService.serie(ticker, periodo, granularidade)));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(mensagemAmigavel(e)));
        }
    }

    /** `RuntimeException` com stack trace de rede não deve ir para o usuário. */
    private String mensagemAmigavel(RuntimeException e) {
        return e.getMessage() == null ? "Cotação indisponível no momento" : e.getMessage();
    }

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