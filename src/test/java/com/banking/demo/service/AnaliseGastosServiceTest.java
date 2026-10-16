package com.banking.demo.service;

import com.banking.demo.model.AnaliseGastos;
import com.banking.demo.model.ComparativoMensal;
import com.banking.demo.model.GastoRecorrente;
import com.banking.demo.model.SugestaoEconomia;
import com.banking.demo.model.Transacao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A análise de gastos é a parte com mais regra deste módulo, então os casos
 * estão todos fixados aqui: o que conta como recorrente, o que vira assinatura
 * e quando a sugestão de economia aparece.
 */
class AnaliseGastosServiceTest {

    private final AnaliseGastosService service = new AnaliseGastosService(new TransacaoService());

    // ------------------------------------------------------------------
    // Comparativo mensal
    // ------------------------------------------------------------------

    @Test
    void comparativo_agrupaPorMesEcalculaVariacaoDeSaidas() {
        List<Transacao> transacoes = List.of(
                saida("Uber", "transporte", 100.0, LocalDate.of(2026, 8, 10)),
                saida("Uber", "transporte", 150.0, LocalDate.of(2026, 9, 10)),
                entrada("Salário", 5000.0, LocalDate.of(2026, 9, 5)));

        AnaliseGastos analise = service.analisar(transacoes);

        // Mais recente primeiro.
        assertEquals("2026-09", analise.getMesAtual());
        assertEquals(2, analise.getComparativo().size());

        ComparativoMensal setembro = analise.getComparativo().get(0);
        assertEquals(150.0, setembro.getSaidas());
        assertEquals(5000.0, setembro.getEntradas());
        assertEquals(4850.0, setembro.getSaldo());
        assertEquals(50.0, setembro.getVariacaoSaidasPercent());

        // O mês mais antigo não tem contra o que comparar.
        assertNull(analise.getComparativo().get(1).getVariacaoSaidasPercent());
    }

    @Test
    void comparativo_primeiroMesSemBase_naoInventaPercentual() {
        AnaliseGastos analise = service.analisar(List.of(
                saida("Uber", "transporte", 100.0, LocalDate.of(2026, 9, 10))));

        assertNull(analise.getComparativo().get(0).getVariacaoSaidasPercent());
    }

    // ------------------------------------------------------------------
    // Recorrência
    // ------------------------------------------------------------------

    @Test
    void recorrentes_valorEstavelTodoMes_viraAssinatura() {
        AnaliseGastos analise = service.analisar(List.of(
                saida("Netflix", "assinaturas", 55.90, LocalDate.of(2026, 7, 8)),
                saida("Netflix", "assinaturas", 55.90, LocalDate.of(2026, 8, 8)),
                saida("Netflix", "assinaturas", 55.90, LocalDate.of(2026, 9, 8))));

        assertEquals(1, analise.getRecorrentes().size());
        GastoRecorrente netflix = analise.getRecorrentes().get(0);

        assertEquals("NETFLIX", netflix.getEstabelecimento());
        assertEquals("ASSINATURA", netflix.getTipo());
        assertEquals(3, netflix.getQuantidadeMeses());
        assertEquals(List.of("2026-07", "2026-08", "2026-09"), netflix.getMeses());
        assertEquals(0, new java.math.BigDecimal("55.90")
                .compareTo(java.math.BigDecimal.valueOf(netflix.getValorMedio())));
        // Cancelando, economizaria 12x o valor médio por ano.
        assertEquals(55.90 * 12, netflix.getEconomiaAnualPotencial(), 0.01);
    }

    @Test
    void recorrentes_valorQueOscila_viraFixoSemEconomiaSugerida() {
        AnaliseGastos analise = service.analisar(List.of(
                saida("Energisa", "moradia", 187.34, LocalDate.of(2026, 7, 15)),
                saida("Energisa", "moradia", 203.11, LocalDate.of(2026, 8, 15)),
                saida("Energisa", "moradia", 231.87, LocalDate.of(2026, 9, 15))));

        GastoRecorrente energisa = analise.getRecorrentes().get(0);

        assertEquals("FIXO", energisa.getTipo());
        // Conta de luz não se cancela: a sugestão não promete economia.
        assertEquals(0.0, energisa.getEconomiaAnualPotencial(), 0.001);
    }

    @Test
    void recorrentes_ignoraGastoQueApareceuEmUmSoMes() {
        AnaliseGastos analise = service.analisar(List.of(
                saida("Cinemark", "lazer", 68.0, LocalDate.of(2026, 9, 28)),
                saida("Cinemark", "lazer", 72.0, LocalDate.of(2026, 9, 29))));

        assertTrue(analise.getRecorrentes().isEmpty());
    }

    @Test
    void recorrentes_normalizaCaixaParaNãoDuplicarALoja() {
        AnaliseGastos analise = service.analisar(List.of(
                saida("Netflix", "assinaturas", 55.90, LocalDate.of(2026, 8, 8)),
                saida("NETFLIX", "assinaturas", 55.90, LocalDate.of(2026, 9, 8))));

        assertEquals(1, analise.getRecorrentes().size());
        assertEquals("NETFLIX", analise.getRecorrentes().get(0).getEstabelecimento());
    }

    @Test
    void recorrentes_transacaoComErro_naoContaComoGasto() {
        Transacao falha = saida("Netflix", "assinaturas", 55.90, LocalDate.of(2026, 7, 8));
        falha.setStatus("ERRO");

        AnaliseGastos analise = service.analisar(List.of(
                falha,
                saida("Netflix", "assinaturas", 55.90, LocalDate.of(2026, 8, 8))));

        // Só um mês válido sobrou -> não é recorrente.
        assertTrue(analise.getRecorrentes().isEmpty());
    }

    // ------------------------------------------------------------------
    // Sugestões
    // ------------------------------------------------------------------

    @Test
    void sugestoes_geraRenovacaoParaAssinatura() {
        AnaliseGastos analise = service.analisar(List.of(
                saida("Spotify", "assinaturas", 21.90, LocalDate.of(2026, 8, 10)),
                saida("Spotify", "assinaturas", 21.90, LocalDate.of(2026, 9, 10))));

        List<SugestaoEconomia> renovacoes = analise.getSugestoes().stream()
                .filter(s -> "RENOVIACAO".equals(s.getTipo()))
                .toList();

        assertEquals(1, renovacoes.size());
        assertTrue(renovacoes.get(0).getTitulo().toUpperCase().contains("SPOTIFY"));
        assertTrue(renovacoes.get(0).getDescricao().contains("por ano"));
        assertEquals(21.90, renovacoes.get(0).getImpactoMensal(), 0.01);
    }

    @Test
    void sugestoes_naoSugereRenovarGastoFixo() {
        // Energia oscila mês a mês -> FIXO, e fixo não se cancela.
        AnaliseGastos analise = service.analisar(List.of(
                saida("Energisa", "moradia", 187.34, LocalDate.of(2026, 8, 15)),
                saida("Energisa", "moradia", 231.87, LocalDate.of(2026, 9, 15))));

        assertEquals("FIXO", analise.getRecorrentes().get(0).getTipo());
        assertTrue(analise.getSugestoes().stream().noneMatch(s -> "RENOVIACAO".equals(s.getTipo())),
                "gasto fixo não deve virar sugestão de cancelamento");
    }

    @Test
    void sugestoes_avisaQuandoOSaldoDoMesCresceForte() {
        AnaliseGastos analise = service.analisar(List.of(
                saida("Mercado", "alimentacao", 600.0, LocalDate.of(2026, 8, 19)),
                saida("Mercado", "alimentacao", 900.0, LocalDate.of(2026, 9, 19))));

        List<SugestaoEconomia> crescimento = analise.getSugestoes().stream()
                .filter(s -> "CRESCIMENTO".equals(s.getTipo()))
                .toList();

        assertEquals(1, crescimento.size());
        assertEquals("ALTA", crescimento.get(0).getPrioridade());
        assertEquals(300.0, crescimento.get(0).getImpactoMensal(), 0.01);
    }

    @Test
    void sugestoes_ordenadasDaMaisImpactanteParaAMenos() {
        AnaliseGastos analise = service.analisar(List.of(
                saida("Netflix", "assinaturas", 55.90, LocalDate.of(2026, 8, 8)),
                saida("Netflix", "assinaturas", 55.90, LocalDate.of(2026, 9, 8)),
                saida("Academia", "saude", 109.90, LocalDate.of(2026, 8, 12)),
                saida("Academia", "saude", 109.90, LocalDate.of(2026, 9, 12))));

        List<SugestaoEconomia> sugestoes = analise.getSugestoes();
        assertFalse(sugestoes.isEmpty());

        for (int i = 1; i < sugestoes.size(); i++) {
            int anterior = prioridade(sugestoes.get(i - 1).getPrioridade());
            int atual = prioridade(sugestoes.get(i).getPrioridade());
            assertTrue(anterior <= atual, "sugestões fora de ordem de prioridade");
        }
    }

    // ------------------------------------------------------------------
    // Resumo
    // ------------------------------------------------------------------

    @Test
    void analise_preencheResumoDoMesAtualEContadores() {
        AnaliseGastos analise = service.analisar(List.of(
                saida("Uber", "transporte", 100.0, LocalDate.of(2026, 9, 10)),
                saida("Mercado", "alimentacao", 400.0, LocalDate.of(2026, 9, 19)),
                entrada("Salário", 5000.0, LocalDate.of(2026, 9, 5))));

        assertEquals("2026-09", analise.getMesAtual());
        assertEquals(5000.0, analise.getEntradasMesAtual());
        assertEquals(500.0, analise.getSaidasMesAtual());
        assertEquals(4500.0, analise.getSaldoMesAtual());
        assertEquals(3, analise.getTransacoesAnalisadas());
        assertEquals(0, analise.getTransacoesAvaliadas());
    }

    @Test
    void analise_contaTransacoesAvaliadas() {
        Transacao avaliada = saida("Uber", "transporte", 100.0, LocalDate.of(2026, 9, 10));
        avaliada.setAvaliacao(4);

        AnaliseGastos analise = service.analisar(List.of(avaliada));

        assertEquals(1, analise.getTransacoesAvaliadas());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static int prioridade(String prioridade) {
        return switch (prioridade) {
            case "ALTA" -> 0;
            case "MEDIA" -> 1;
            default -> 2;
        };
    }

    private static Transacao saida(String estabelecimento, String categoria, double valor, LocalDate data) {
        Transacao t = new Transacao();
        t.setDescricao("gasto em " + estabelecimento);
        t.setEstabelecimento(estabelecimento);
        t.setCategoria(categoria);
        t.setMetodo("PIX");
        t.setTipo("saida");
        t.setValor(valor);
        t.setData(data);
        return t;
    }

    private static Transacao entrada(String estabelecimento, double valor, LocalDate data) {
        Transacao t = new Transacao();
        t.setDescricao("recebimento");
        t.setEstabelecimento(estabelecimento);
        t.setCategoria("salario");
        t.setMetodo("TED");
        t.setTipo("entrada");
        t.setValor(valor);
        t.setData(data);
        return t;
    }
}
