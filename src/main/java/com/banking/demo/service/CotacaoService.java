package com.banking.demo.service;

import com.banking.demo.model.Cotacao;
import com.banking.demo.model.PontoSerie;
import com.banking.demo.model.SerieCotacao;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Cotações de mercado vindas do Yahoo Finance.
 *
 * O frontend Angular fala só com esta API e nunca direto com o provedor de
 * mercado. Isso resolve três problemas reais:
 *
 * 1. **Sem token.** A BrAPI passou a devolver `401` sem token e limita a 20
 *    consultas/minuto; o Yahoo responde sem autenticação.
 * 2. **Sem CORS.** O navegador não fala com o Yahoo direto (sem
 *    `access-control-allow-origin`), mas o servidor Java não depende de CORS.
 * 3. **Sem regra duplicada.** A normalização (sufixo `.SA`, variação vs.
 *    fechamento anterior, data em `AAAA-MM-DD`) vive só aqui.
 *
 * O resultado é cacheado por 60s para não estourar o rate limit do provedor
 * quando a tela de cotações carrega 6 ativos de uma vez.
 */
@Service
public class CotacaoService {

    /** Yahoo Finance, versão de chart: devolve preço + histórico num único GET. */
    private static final String URL_BASE = "https://query1.finance.yahoo.com/v8/finance/chart/";

    /** Cache por ticker: tela aberta 10 vezes em 1 minuto = 1 chamada externa. */
    private static final long TTL_MS = 60_000;

    /** Teto defensivo para não virar um burst de chamadas externas. */
    private static final int MAX_TICHERS = 20;

    /** Mercado brasileiro no Yahoo: PETR4 -> PETR4.SA. */
    private static final Pattern MERCADO_BRASILEIRO = Pattern.compile("[A-Z]{4}\\d{1,2}");

    private static final ZoneId FUSO_BOLSA = ZoneId.of("America/Sao_Paulo");

    private final RestTemplate restTemplate;
    private final Map<String, EntradaCache> cache = new ConcurrentHashMap<>();

    public CotacaoService() {
        this(criarRestTemplate());
    }

    /** Somente para teste: injeta o cliente HTTP sem sair para a rede. */
    CotacaoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Lote de cotações, num GET só (o provedor recebe uma requisição por ticker).
     *
     * Um ticker inexistente não pode derrubar o lote inteiro: as cotações que
     * responderem são devolvidas e só falha quando nenhuma responde.
     */
    public List<Cotacao> cotacoes(List<String> tickers) {
        List<Cotacao> cotacoes = new ArrayList<>();
        RuntimeException primeiraFalha = null;

        for (String ticker : tickers) {
            if (cotacoes.size() >= MAX_TICHERS) {
                break;
            }
            try {
                cotacoes.add(cotacao(ticker));
            } catch (RuntimeException falha) {
                primeiraFalha = primeiraFalha == null ? falha : primeiraFalha;
            }
        }

        if (cotacoes.isEmpty() && primeiraFalha != null) {
            throw primeiraFalha;
        }
        return cotacoes;
    }

    /** Cotação de um ativo, com cache de 60s. Lança `RuntimeException` se não houver dado. */
    public Cotacao cotacao(String ticker) {
        String simbolo = normalizarTicker(ticker);

        EntradaCache entrada = cache.get(simbolo);
        if (entrada != null && System.currentTimeMillis() - entrada.lidoEm() < TTL_MS) {
            return (Cotacao) entrada.valor();
        }

        Map<String, Object> raiz = buscar(simbolo, "1d", "1d");
        Cotacao cotacao = mapearCotacao(simbolo, raiz);

        cache.put(simbolo, new EntradaCache(cotacao, System.currentTimeMillis()));
        return cotacao;
    }

    /** Série histórica para o gráfico/sparkline. Sem cache: a tela pede períodos diferentes. */
    public SerieCotacao serie(String ticker, String range, String interval) {
        String simbolo = normalizarTicker(ticker);
        String intervalo = interval == null || interval.isBlank() ? "1d" : interval.trim();

        Map<String, Object> raiz = buscar(simbolo, range, intervalo);
        Map<String, Object> resultado = primeiroResultado(raiz);
        Map<String, Object> meta = mapa(resultado.get("meta"));

        SerieCotacao serie = new SerieCotacao();
        serie.setTicker(simbolo);
        serie.setMoeda(texto(meta.get("currency"), "BRL"));
        serie.setRange(range);
        serie.setInterval(intervalo);
        serie.setPontos(mapearPontos(resultado, meta));
        return serie;
    }

    // ------------------------------------------------------------------
    // Chamada HTTP
    // ------------------------------------------------------------------

    /**
     * Consulta o Yahoo e devolve o mapa `{ chart: { result: [...] } }`.
     * Ticker inexistente ou provedor fora do ar viram `RuntimeException` com
     * mensagem amigável: o frontend então cai no dado de demonstração.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buscar(String simbolo, String range, String interval) {
        String url = URL_BASE + simboloYahoo(simbolo)
                + "?range=" + range + "&interval=" + interval;

        Map<String, Object> corpo;
        try {
            corpo = restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Provedor de cotações indisponível: " + e.getMessage(), e);
        }

        Map<String, Object> chart = mapa(corpo.get("chart"));
        Map<String, Object> erro = mapa(chart.get("error"));
        if (erro.containsKey("description")) {
            throw new RuntimeException("Sem cotação para " + simbolo + ": "
                    + texto(erro.get("description"), "ativo não encontrado"));
        }

        List<Map<String, Object>> resultados = lista(chart.get("result"));
        if (resultados.isEmpty()) {
            throw new RuntimeException("Sem cotação para " + simbolo);
        }
        return corpo;
    }

    // ------------------------------------------------------------------
    // Mapeamento: Yahoo -> modelo do domínio (formato que o Angular espera)
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Cotacao mapearCotacao(String simbolo, Map<String, Object> raiz) {
        Map<String, Object> resultado = primeiroResultado(raiz);
        Map<String, Object> meta = mapa(resultado.get("meta"));

        BigDecimal preco = decimal(meta.get("regularMarketPrice"), BigDecimal.ZERO);
        BigDecimal anterior = decimal(meta.get("chartPreviousClose"),
                decimal(meta.get("previousClose"), preco));

        // O chart não traz variação pronta sempre: usa o valor do provedor e,
        // se faltar, calcula contra o fechamento anterior.
        BigDecimal variacao = decimal(meta.get("fulldayChange"), preco.subtract(anterior));
        BigDecimal variacaoPercent = decimal(meta.get("fulldayChangePercent"),
                percentual(preco, anterior));

        Cotacao cotacao = new Cotacao();
        cotacao.setTicker(simbolo);
        cotacao.setNome(texto(meta.get("longName"), texto(meta.get("shortName"), simbolo)));
        cotacao.setPreco(preco);
        cotacao.setVariacao(variacao.setScale(2, RoundingMode.HALF_UP));
        cotacao.setVariacaoPercent(variacaoPercent.setScale(2, RoundingMode.HALF_UP));
        cotacao.setMoeda(texto(meta.get("currency"), "BRL"));
        cotacao.setAbertura(abertura(resultado));
        cotacao.setMinima(decimal(meta.get("regularMarketDayLow"), preco));
        cotacao.setMaxima(decimal(meta.get("regularMarketDayHigh"), preco));
        cotacao.setVolume(longo(meta.get("regularMarketVolume"), 0L));
        // O endpoint gratuito de chart não expõe market cap (o de quote exige crumb).
        cotacao.setValorMercado(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        cotacao.setMinimo52(decimal(meta.get("fiftyTwoWeekLow"), preco));
        cotacao.setMaximo52(decimal(meta.get("fiftyTwoWeekHigh"), preco));
        cotacao.setAtualizadoEm(iso(meta.get("regularMarketTime")));
        cotacao.setLogoUrl("");
        return cotacao;
    }

    @SuppressWarnings("unchecked")
    private List<PontoSerie> mapearPontos(Map<String, Object> resultado, Map<String, Object> meta) {
        List<PontoSerie> pontos = new ArrayList<>();

        List<Number> carimbos = numeros(resultado.get("timestamp"));
        List<Number> fechamentos = new ArrayList<>();
        Map<String, Object> indicadores = mapa(resultado.get("indicators"));
        List<Map<String, Object>> cotacoes = lista(indicadores.get("quote"));
        if (!cotacoes.isEmpty()) {
            fechamentos = numeros(cotacoes.get(0).get("close"));
        }

        // A data é a da bolsa, não a do servidor: 00h UTC não é o mesmo pregão.
        ZoneId fuso = fuso(meta);
        for (int i = 0; i < carimbos.size() && i < fechamentos.size(); i++) {
            Number fechamento = fechamentos.get(i);
            if (fechamento == null) {
                continue; // feriado/fim de semana: barra sem pregão
            }
            String data = ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(carimbos.get(i).longValue()), fuso).toLocalDate().toString();
            pontos.add(new PontoSerie(data, BigDecimal.valueOf(fechamento.doubleValue())
                    .setScale(2, RoundingMode.HALF_UP)));
        }
        return pontos;
    }

    @SuppressWarnings("unchecked")
    private BigDecimal abertura(Map<String, Object> resultado) {
        Map<String, Object> indicadores = mapa(resultado.get("indicators"));
        List<Map<String, Object>> cotacoes = lista(indicadores.get("quote"));
        if (cotacoes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // A última barra é o pregão corrente; as anteriores são histórico.
        List<Number> aberturas = numeros(cotacoes.get(0).get("open"));
        for (int i = aberturas.size() - 1; i >= 0; i--) {
            if (aberturas.get(i) != null) {
                return BigDecimal.valueOf(aberturas.get(i).doubleValue());
            }
        }
        return BigDecimal.ZERO;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /** `petr4` e `PETR4` são o mesmo ativo; devolve sempre maiúsculo. */
    private String normalizarTicker(String ticker) {
        return ticker == null ? "" : ticker.trim().toUpperCase();
    }

    /** Mercado brasileiro ganha `.SA` no Yahoo (PETR4 -> PETR4.SA). */
    private String simboloYahoo(String simbolo) {
        if (simbolo.contains(".")) {
            return simbolo;
        }
        return MERCADO_BRASILEIRO.matcher(simbolo).matches() ? simbolo + ".SA" : simbolo;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> primeiroResultado(Map<String, Object> raiz) {
        Map<String, Object> chart = mapa(raiz.get("chart"));
        return lista(chart.get("result")).get(0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapa(Object valor) {
        return valor instanceof Map ? (Map<String, Object>) valor : Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> lista(Object valor) {
        return valor instanceof List ? (List<Map<String, Object>>) valor : List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Number> numeros(Object valor) {
        return valor instanceof List ? (List<Number>) valor : List.of();
    }

    private BigDecimal decimal(Object valor, BigDecimal padrao) {
        return valor instanceof Number ? BigDecimal.valueOf(((Number) valor).doubleValue()) : padrao;
    }

    private Long longo(Object valor, long padrao) {
        return valor instanceof Number ? ((Number) valor).longValue() : padrao;
    }

    private String texto(Object valor, String padrao) {
        return valor instanceof String && !((String) valor).isBlank() ? (String) valor : padrao;
    }

    private BigDecimal percentual(BigDecimal atual, BigDecimal anterior) {
        if (anterior == null || anterior.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return atual.subtract(anterior).multiply(BigDecimal.valueOf(100)).divide(anterior, 4, RoundingMode.HALF_UP);
    }

    private String iso(Object epochSegundos) {
        long segundos = longo(epochSegundos, System.currentTimeMillis() / 1000);
        return Instant.ofEpochSecond(segundos).toString();
    }

    @SuppressWarnings("unchecked")
    private ZoneId fuso(Map<String, Object> meta) {
        Object nome = meta.get("exchangeTimezoneName");
        if (nome instanceof String && !((String) nome).isBlank()) {
            try {
                return ZoneId.of((String) nome);
            } catch (Exception ignorado) {
                return FUSO_BOLSA;
            }
        }
        return FUSO_BOLSA;
    }

    private static RestTemplate criarRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // O Yahoo responde rápido, mas o usuário não deve esperar: sem timeout a
        // tela de cotações ficaria pendurada quando o provedor engasga.
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(8).toMillis());

        RestTemplate cliente = new RestTemplate(factory);
        cliente.setInterceptors(List.of((request, body, execution) -> {
            // Sem User-Agent o Yahoo responde 400/429 na maioria das regiões.
            request.getHeaders().set(HttpHeaders.USER_AGENT,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) java-spring-boot-banking-demo");
            request.getHeaders().set(HttpHeaders.ACCEPT, "application/json");
            return execution.execute(request, body);
        }));
        return cliente;
    }

    /** Entrada do cache: valor pronto + quando foi lido. */
    private record EntradaCache(Object valor, long lidoEm) {
    }
}
