package com.banking.demo.service;

import com.banking.demo.model.AnaliseGastos;
import com.banking.demo.model.ComparativoMensal;
import com.banking.demo.model.GastoRecorrente;
import com.banking.demo.model.SugestaoEconomia;
import com.banking.demo.model.Transacao;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Análise de gastos sobre o extrato: comparativo mensal, gastos recorrentes e
 * sugestões de economia.
 *
 * Tudo é calculado a partir dos dados que já existem — não há IA nem
 * integração externa aqui. As regras são deliberadamente simples e explicáveis
 * ("você pagou X nos últimos 3 meses"), porque o usuário precisa entender de
 * onde veio cada sugestão.
 */
@Service
public class AnaliseGastosService {

    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("yyyy-MM");

    /** Abaixo disso o valor é considerado estável -> assinatura. */
    private static final double LIMIAR_VARIABILIDADE_ASSINATURA = 0.05;

    /** Mínimo de meses distintos para um gasto valer como recorrente. */
    private static final int MINIMO_MESES_RECORRENTE = 2;

    /** Variação de saídas que dispara a sugestão de atenção. */
    private static final double LIMIAR_CRESCIMENTO_PERCENT = 15.0;

    /** Ordena sugestões: ALTA antes de MEDIA antes de BAIXA. */
    private static final Comparator<String> ORDEM_PRIORIDADE =
            (a, b) -> Integer.compare(ordemPrioridade(a), ordemPrioridade(b));

    private final TransacaoService transacaoService;

    public AnaliseGastosService(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    public AnaliseGastos analisar() {
        return analisar(transacaoService.todas());
    }

    /**
     * Puro: recebe as transações e devolve a análise. Separado do construtor
     * para permitir teste sem Spring e sem dados de exemplo do serviço.
     */
    public AnaliseGastos analisar(List<Transacao> todas) {
        List<Transacao> validas = todas.stream()
                .filter(t -> t.getData() != null && t.getValor() != null)
                .toList();

        List<Transacao> saidas = validas.stream()
                .filter(AnaliseGastosService::ehSaida)
                .toList();

        List<ComparativoMensal> comparativo = comparativo(validas);
        List<GastoRecorrente> recorrentes = recorrentes(saidas);
        List<SugestaoEconomia> sugestoes = sugestoes(comparativo, recorrentes);

        AnaliseGastos analise = new AnaliseGastos();
        analise.setComparativo(comparativo);
        analise.setRecorrentes(recorrentes);
        analise.setSugestoes(sugestoes);
        analise.setTransacoesAnalisadas(validas.size());
        analise.setTransacoesAvaliadas(
                (int) validas.stream().filter(t -> t.getAvaliacao() != null).count());

        if (!comparativo.isEmpty()) {
            ComparativoMensal atual = comparativo.get(0);
            analise.setMesAtual(atual.getMes());
            analise.setEntradasMesAtual(atual.getEntradas());
            analise.setSaidasMesAtual(atual.getSaidas());
            analise.setSaldoMesAtual(atual.getSaldo());
        }
        return analise;
    }

    // ------------------------------------------------------------------
    // Comparativo mensal
    // ------------------------------------------------------------------

    /** Agrupa por mês e calcula a variação contra o mês anterior. */
    private List<ComparativoMensal> comparativo(List<Transacao> transacoes) {
        Map<String, double[]> porMes = new HashMap<>();

        for (Transacao t : transacoes) {
            String mes = t.getData().format(MES);
            // [0] entradas, [1] saídas
            double[] acumulado = porMes.computeIfAbsent(mes, chave -> new double[2]);
            if (ehSaida(t)) {
                acumulado[1] += Math.abs(t.getValor());
            } else {
                acumulado[0] += Math.abs(t.getValor());
            }
        }

        List<String> meses = new ArrayList<>(porMes.keySet());
        Collections.sort(meses); // cronológico para a variação fazer sentido

        List<ComparativoMensal> resultado = new ArrayList<>();
        ComparativoMensal anterior = null;

        for (String mes : meses) {
            double[] v = porMes.get(mes);
            ComparativoMensal atual = new ComparativoMensal();
            atual.setMes(mes);
            atual.setEntradas(v[0]);
            atual.setSaidas(v[1]);
            atual.setSaldo(v[0] - v[1]);
            if (anterior != null) {
                atual.setVariacaoSaidasPercent(variacaoPercent(anterior.getSaidas(), v[1]));
                atual.setVariacaoEntradasPercent(variacaoPercent(anterior.getEntradas(), v[0]));
            }
            resultado.add(atual);
            anterior = atual;
        }

        Collections.reverse(resultado); // mais recente primeiro
        return resultado;
    }

    // ------------------------------------------------------------------
    // Gastos recorrentes
    // ------------------------------------------------------------------

    /**
     * Acha o "gasto padrão": mesmo estabelecimento em mais de um mês.
     *
     * Com valor quase idêntico todo mês vira `ASSINATURA` (streaming, academia);
     * com valor que oscila vira `FIXO` (mercado, luz). A normalização por
     * caixa evita que `Netflix` e `NETFLIX` virem dois gastos diferentes.
     */
    private List<GastoRecorrente> recorrentes(List<Transacao> saidas) {
        Map<String, List<Transacao>> porEstabelecimento = saidas.stream()
                .filter(t -> t.getEstabelecimento() != null && !t.getEstabelecimento().isBlank())
                .collect(Collectors.groupingBy(t -> normalizarEstabelecimento(t.getEstabelecimento())));

        List<GastoRecorrente> resultado = new ArrayList<>();

        for (Map.Entry<String, List<Transacao>> entrada : porEstabelecimento.entrySet()) {
            List<String> meses = entrada.getValue().stream()
                    .map(t -> t.getData().format(MES))
                    .distinct()
                    .sorted()
                    .toList();

            if (meses.size() < MINIMO_MESES_RECORRENTE) {
                continue;
            }

            List<Double> valores = entrada.getValue().stream()
                    .map(t -> Math.abs(t.getValor()))
                    .toList();
            double media = media(valores);
            boolean estavel = coeficienteVariacao(valores) < LIMIAR_VARIABILIDADE_ASSINATURA;

            GastoRecorrente gasto = new GastoRecorrente();
            gasto.setEstabelecimento(entrada.getKey());
            gasto.setCategoria(categoriaPrincipal(entrada.getValue()));
            gasto.setTipo(estavel ? "ASSINATURA" : "FIXO");
            gasto.setValorMedio(media);
            gasto.setQuantidadeMeses(meses.size());
            gasto.setMeses(meses);
            gasto.setTotalGasto(soma(valores));
            gasto.setEconomiaAnualPotencial(estavel ? media * 12 : 0.0);
            resultado.add(gasto);
        }

        resultado.sort(Comparator.comparing(GastoRecorrente::getValorMedio).reversed());
        return resultado;
    }

    // ------------------------------------------------------------------
    // Sugestões de economia
    // ------------------------------------------------------------------

    private List<SugestaoEconomia> sugestoes(List<ComparativoMensal> comparativo,
                                             List<GastoRecorrente> recorrentes) {
        List<SugestaoEconomia> sugestoes = new ArrayList<>();

        for (GastoRecorrente gasto : recorrentes) {
            if (!"ASSINATURA".equals(gasto.getTipo())) {
                continue; // FIXO não se cancela: luz e mercado voltam todo mês
            }
            SugestaoEconomia sugestao = new SugestaoEconomia();
            sugestao.setTipo("RENOVIACAO");
            sugestao.setTitulo("Revise a assinatura de " + gasto.getEstabelecimento());
            sugestao.setDescricao(String.format(Locale.of("pt", "BR"),
                    "Você pagou em média R$ %.2f/mês com %s nos últimos %d meses — cerca de R$ %.2f por ano. "
                            + "Cancele ou renegocie se não estiver usando.",
                    gasto.getValorMedio(), gasto.getEstabelecimento(),
                    gasto.getQuantidadeMeses(), gasto.getEconomiaAnualPotencial()));
            sugestao.setImpactoMensal(gasto.getValorMedio());
            sugestao.setPrioridade(gasto.getValorMedio() >= 50.0 ? "ALTA" : "MEDIA");
            sugestoes.add(sugestao);
        }

        // O mês atual cresceu forte em relação ao anterior? Avisa.
        if (comparativo.size() >= 2) {
            ComparativoMensal atual = comparativo.get(0);
            ComparativoMensal anterior = comparativo.get(1);
            Double variacao = atual.getVariacaoSaidasPercent();

            if (variacao != null && variacao > LIMIAR_CRESCIMENTO_PERCENT) {
                SugestaoEconomia sugestao = new SugestaoEconomia();
                sugestao.setTipo("CRESCIMENTO");
                sugestao.setTitulo("Gastos subiram " + String.format(Locale.of("pt", "BR"),
                        "%.0f%%", variacao) + " em " + atual.getMes());
                sugestao.setDescricao(String.format(Locale.of("pt", "BR"),
                        "Em %s você gastou R$ %.2f, contra R$ %.2f em %s — uma diferença de R$ %.2f. "
                                + "Confira se há gasto pontual ou algo que virou recorrente.",
                        atual.getMes(), atual.getSaidas(), anterior.getSaidas(),
                        anterior.getMes(), atual.getSaidas() - anterior.getSaidas()));
                sugestao.setImpactoMensal(atual.getSaidas() - anterior.getSaidas());
                sugestao.setPrioridade("ALTA");
                sugestoes.add(sugestao);
            }
        }

        sugestoes.sort(Comparator
                .comparing(SugestaoEconomia::getPrioridade, ORDEM_PRIORIDADE)
                .thenComparing(SugestaoEconomia::getImpactoMensal, Comparator.reverseOrder()));
        return sugestoes;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static boolean ehSaida(Transacao t) {
        // Transação com status ERRO não moveu dinheiro de verdade.
        return "saida".equalsIgnoreCase(t.getTipo())
                && !"ERRO".equalsIgnoreCase(t.getStatus());
    }

    private static String normalizarEstabelecimento(String estabelecimento) {
        return estabelecimento.trim().toUpperCase(Locale.of("pt", "BR"));
    }

    /** Categoria que mais aparece no grupo; `outros` quando não houver. */
    private static String categoriaPrincipal(List<Transacao> transacoes) {
        return transacoes.stream()
                .map(t -> t.getCategoria() == null || t.getCategoria().isBlank()
                        ? "outros" : t.getCategoria().trim().toLowerCase(Locale.of("pt", "BR")))
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("outros");
    }

    private static Double variacaoPercent(double anterior, double atual) {
        if (anterior == 0) {
            return null; // sem base de comparação: não inventa porcentagem
        }
        return ((atual - anterior) / Math.abs(anterior)) * 100.0;
    }

    private static double media(List<Double> valores) {
        return valores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private static double soma(List<Double> valores) {
        return valores.stream().mapToDouble(Double::doubleValue).sum();
    }

    /** Desvio padrão dividido pela média: 0 = valor idêntico todo mês. */
    private static double coeficienteVariacao(List<Double> valores) {
        double media = media(valores);
        if (media == 0) {
            return 1.0;
        }
        double variancia = valores.stream()
                .mapToDouble(v -> Math.pow(v - media, 2))
                .sum() / valores.size();
        return Math.sqrt(variancia) / media;
    }

    private static int ordemPrioridade(String prioridade) {
        return switch (prioridade == null ? "" : prioridade) {
            case "ALTA" -> 0;
            case "MEDIA" -> 1;
            default -> 2;
        };
    }
}
