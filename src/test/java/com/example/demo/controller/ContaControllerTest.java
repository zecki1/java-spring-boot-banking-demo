package com.example.demo.controller;

import com.example.demo.model.ApiResponse;
import com.example.demo.model.Conta;
import com.example.demo.service.ContaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContaControllerTest {

    @Mock
    private ContaService contaService;

    @InjectMocks
    private ContaController contaController;

    private Conta conta;

    @BeforeEach
    void setUp() {
        conta = new Conta(1L, "0001-1", "João Silva", "12345678900", 
                new BigDecimal("5000.00"), "0001");
    }

    @Test
    void listarContas_deveRetornarListaDeContas() {
        List<Conta> contas = Arrays.asList(conta);
        when(contaService.listarTodas()).thenReturn(contas);

        ResponseEntity<ApiResponse<List<Conta>>> response = contaController.listarContas();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getCount());
        verify(contaService, times(1)).listarTodas();
    }

    @Test
    void criarConta_comDadosValidos_deveRetornarCreated() {
        when(contaService.criar(any(Conta.class))).thenReturn(conta);

        Conta novaConta = new Conta();
        novaConta.setTitular("João Silva");
        novaConta.setSaldoInicial(new BigDecimal("1000.00"));

        ResponseEntity<ApiResponse<Conta>> response = contaController.criarConta(novaConta);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void criarConta_semTitular_deveRetornarBadRequest() {
        Conta novaConta = new Conta();
        novaConta.setSaldoInicial(new BigDecimal("1000.00"));

        ResponseEntity<ApiResponse<Conta>> response = contaController.criarConta(novaConta);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
    }
}