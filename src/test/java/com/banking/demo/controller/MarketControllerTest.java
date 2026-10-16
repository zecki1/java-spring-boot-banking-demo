package com.banking.demo.controller;

import com.banking.demo.model.ApiResponse;
import com.banking.demo.model.Cotacao;
import com.banking.demo.model.SerieCotacao;
import com.banking.demo.service.CotacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketControllerTest {

    @Mock
    private CotacaoService cotacaoService;

    @InjectMocks
    private MarketController marketController;

    private Cotacao petr4;

    @BeforeEach
    void setUp() {
        petr4 = new Cotacao();
        petr4.setTicker("PETR4");
        petr4.setNome("Petróleo Brasileiro S.A. - Petrobras");
        petr4.setPreco(new BigDecimal("57.91"));
        petr4.setMoeda("BRL");
    }

    @Test
    void cotacoes_comTickersValidos_deveRetornarLista() {
        when(cotacaoService.cotacoes(List.of("PETR4", "VALE3"))).thenReturn(List.of(petr4));

        ResponseEntity<ApiResponse<List<Cotacao>>> response =
                marketController.cotacoes("PETR4,VALE3");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getCount());
        assertEquals("PETR4", response.getBody().getData().get(0).getTicker());
    }

    @Test
    void cotacoes_semTickers_deveRetornarBadRequest() {
        ResponseEntity<ApiResponse<List<Cotacao>>> response = marketController.cotacoes(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertNotNull(response.getBody().getMensagem());
        verifyNoInteractions(cotacaoService);
    }

    @Test
    void cotacoes_somenteComEspacos_deveRetornarBadRequest() {
        ResponseEntity<ApiResponse<List<Cotacao>>> response = marketController.cotacoes(" , , ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
    }

    @Test
    void cotacoes_normalizaCaixaERemoveRepetidos() {
        when(cotacaoService.cotacoes(List.of("PETR4", "VALE3"))).thenReturn(List.of(petr4));

        marketController.cotacoes("petr4, PETR4 , vale3");

        verify(cotacaoService).cotacoes(List.of("PETR4", "VALE3"));
    }

    @Test
    void cotacoes_quandoNenhumAtivoResponde_deveDevolverSuccessFalso() {
        when(cotacaoService.cotacoes(List.of("ZZZZ99")))
                .thenThrow(new RuntimeException("Sem cotação para ZZZZ99"));

        ResponseEntity<ApiResponse<List<Cotacao>>> response = marketController.cotacoes("ZZZZ99");

        // 200 e não 5xx: a tela deve cair no dado de demonstração, não em erro vermelho.
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMensagem().contains("ZZZZ99"));
    }

    @Test
    void serie_comTicker_deveRetornarSucesso() {
        SerieCotacao serie = new SerieCotacao();
        serie.setTicker("PETR4");
        when(cotacaoService.serie("PETR4", "1mo", "1d")).thenReturn(serie);

        ResponseEntity<ApiResponse<SerieCotacao>> response =
                marketController.serie("PETR4", "1mo", "1d");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("PETR4", response.getBody().getData().getTicker());
    }

    @Test
    void serie_comDefaults_deveUsarUmMesEDiario() {
        when(cotacaoService.serie("PETR4", "1mo", "1d")).thenReturn(new SerieCotacao());

        marketController.serie("PETR4", null, null);

        verify(cotacaoService).serie("PETR4", "1mo", "1d");
    }

    @Test
    void serie_semTicker_deveRetornarBadRequest() {
        ResponseEntity<ApiResponse<SerieCotacao>> response = marketController.serie("  ", "1mo", "1d");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        verifyNoInteractions(cotacaoService);
    }

    @Test
    void serie_quandoProvedorFalha_deveDevolverSuccessFalso() {
        when(cotacaoService.serie("PETR4", "1mo", "1d"))
                .thenThrow(new RuntimeException("Provedor de cotações indisponível"));

        ResponseEntity<ApiResponse<SerieCotacao>> response =
                marketController.serie("PETR4", "1mo", "1d");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMensagem().contains("indisponível"));
    }
}
