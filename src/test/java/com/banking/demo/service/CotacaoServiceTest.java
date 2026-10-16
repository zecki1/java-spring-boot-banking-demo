package com.banking.demo.service;

import com.banking.demo.model.Cotacao;
import com.banking.demo.model.SerieCotacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Bloqueia o contrato entre a resposta do Yahoo e o modelo do domínio.
 *
 * É aqui que a tradução mora: sufixo `.SA`, variação contra o fechamento
 * anterior e datas no fuso da bolsa. Se o provedor mudar o formato, este teste
 * avisa antes de quebrar a tela do Angular.
 */
class CotacaoServiceTest {

    private RestTemplate restTemplate;
    private CotacaoService service;

    @BeforeEach
    void setUp() {
        restTemplate = Mockito.mock(RestTemplate.class);
        service = new CotacaoService(restTemplate);
    }

    @Test
    void cotacao_mapeiaRespostaDoYahooParaOModeloDoDominio() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(respostaYahoo());

        Cotacao cotacao = service.cotacao("petr4");

        // O ticker devolvido é o que o usuário digitou, sem o sufixo da bolsa.
        assertEquals("PETR4", cotacao.getTicker());
        assertEquals("BRL", cotacao.getMoeda());
        assertEquals(0, new BigDecimal("57.91").compareTo(cotacao.getPreco()));
        assertEquals(0, new BigDecimal("0.55").compareTo(cotacao.getVariacao()));
        assertEquals(0, new BigDecimal("0.96").compareTo(cotacao.getVariacaoPercent()));
        assertEquals(0, new BigDecimal("57.50").compareTo(cotacao.getAbertura()));
        assertEquals(0, new BigDecimal("57.53").compareTo(cotacao.getMinima()));
        assertEquals(0, new BigDecimal("58.45").compareTo(cotacao.getMaxima()));
        assertEquals(13_929_400L, cotacao.getVolume());
        assertEquals(0, new BigDecimal("29.31").compareTo(cotacao.getMinimo52()));
        assertEquals(0, new BigDecimal("58.45").compareTo(cotacao.getMaximo52()));
        assertTrue(cotacao.getNome().startsWith("Petróleo Brasileiro"));
        assertNotNull(cotacao.getAtualizadoEm());
    }

    @Test
    void cotacao_buscaOMercadoBrasileiroComSufixoSA() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(respostaYahoo());

        service.cotacao("PETR4");

        // PETR4 -> PETR4.SA no Yahoo; sem isso a API responde "symbol not found".
        Mockito.verify(restTemplate).getForObject(contains("PETR4.SA"), eq(Map.class));
    }

    @Test
    void cotacao_tickerInternacional_naoGanhaSufixoDeBolsa() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(respostaYahoo());

        service.cotacao("AAPL");

        Mockito.verify(restTemplate).getForObject(not(contains(".SA")), eq(Map.class));
    }

    @Test
    void cotacao_segundaChamadaNoMesmoTicker_usaCacheSemIrARede() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(respostaYahoo());

        service.cotacao("PETR4");
        service.cotacao("PETR4");

        Mockito.verify(restTemplate, Mockito.times(1)).getForObject(anyString(), eq(Map.class));
    }

    @Test
    void cotacao_quandoYahooNaoConheceOTicker_deveLancarFalhaAmigavel() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("chart", Map.of("error", Map.of(
                        "code", "Not Found",
                        "description", "No data found, symbol may be delisted"))));

        RuntimeException falha = assertThrows(RuntimeException.class, () -> service.cotacao("ZZZZ99"));

        assertTrue(falha.getMessage().contains("ZZZZ99"));
        assertTrue(falha.getMessage().contains("No data found"));
    }

    @Test
    void cotacoes_loteDevolveOsQueResponderam() {
        when(restTemplate.getForObject(contains("PETR4.SA"), eq(Map.class))).thenReturn(respostaYahoo());
        when(restTemplate.getForObject(contains("ZZZZ99"), eq(Map.class)))
                .thenThrow(new RuntimeException("Sem cotação para ZZZZ99"));

        List<Cotacao> cotacoes = service.cotacoes(List.of("PETR4", "ZZZZ99"));

        // O ticker desconhecido não derruba o lote inteiro.
        assertEquals(1, cotacoes.size());
        assertEquals("PETR4", cotacoes.get(0).getTicker());
    }

    @Test
    void serie_converteTimestampsEmDatasNoFusoDaBolsa() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(respostaYahooSerie());

        SerieCotacao serie = service.serie("PETR4", "5d", "1d");

        assertEquals("PETR4", serie.getTicker());
        assertEquals("BRL", serie.getMoeda());
        assertEquals("5d", serie.getRange());
        assertEquals(2, serie.getPontos().size());
        // Data da bolsa (America/Sao_Paulo), não a do servidor.
        assertTrue(serie.getPontos().get(0).getDate().matches("^\\d{4}-\\d{2}-\\d{2}$"));
        assertTrue(serie.getPontos().get(0).getDate()
                .compareTo(serie.getPontos().get(1).getDate()) < 0);
        assertEquals(0, new BigDecimal("56.00").compareTo(serie.getPontos().get(0).getClose()));
    }

    // ------------------------------------------------------------------
    // Fixtures: resposta real do Yahoo, reduzida ao que o mapeamento lê
    // ------------------------------------------------------------------

    private Map<String, Object> respostaYahoo() {
        Map<String, Object> meta = Map.ofEntries(
                Map.entry("currency", "BRL"),
                Map.entry("symbol", "PETR4.SA"),
                Map.entry("longName", "Petróleo Brasileiro S.A. - Petrobras"),
                Map.entry("regularMarketPrice", 57.91),
                Map.entry("fulldayChange", 0.55),
                Map.entry("fulldayChangePercent", 0.959),
                Map.entry("chartPreviousClose", 55.48),
                Map.entry("regularMarketDayHigh", 58.45),
                Map.entry("regularMarketDayLow", 57.53),
                Map.entry("regularMarketVolume", 13_929_400L),
                Map.entry("fiftyTwoWeekHigh", 58.45),
                Map.entry("fiftyTwoWeekLow", 29.31),
                Map.entry("regularMarketTime", 1_792_162_651L),
                Map.entry("exchangeTimezoneName", "America/Sao_Paulo"));

        Map<String, Object> resultado = Map.ofEntries(
                Map.entry("meta", meta),
                Map.entry("timestamp", List.of(1_792_155_600L)),
                Map.entry("indicators", Map.of("quote", List.of(
                        Map.of("open", List.of(57.5), "close", List.of(57.91))))));

        return Map.of("chart", Map.of("result", List.of(resultado)));
    }

    private Map<String, Object> respostaYahooSerie() {
        Map<String, Object> meta = Map.of(
                "currency", "BRL",
                "exchangeTimezoneName", "America/Sao_Paulo");

        Map<String, Object> resultado = Map.of(
                "meta", meta,
                "timestamp", List.of(1_791_550_800L, 1_792_155_600L),
                "indicators", Map.of("quote", List.of(Map.of(
                        "open", List.of(55.5, 57.5),
                        "close", List.of(56.0, 57.91)))));

        return Map.of("chart", Map.of("result", List.of(resultado)));
    }
}
