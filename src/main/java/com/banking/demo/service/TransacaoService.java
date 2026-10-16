package com.banking.demo.service;

import com.banking.demo.model.AvaliacaoEstabelecimento;
import com.banking.demo.model.ApiResponse;
import com.banking.demo.model.Transacao;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service layer para lógica de negócio das transações.
 *
 * Guarda tudo em memória (sem banco) — é um demo. Os dados de exemplo cobrem
 * três meses para que a análise de gastos tenha recorrência de verdade para
 * mostrar: assinatura de valor estável, conta variável e gasto pontual.
 */
@Service
public class TransacaoService {

    private final List<Transacao> transacoes = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public TransacaoService() {
        popularDadosDeExemplo();
    }

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    /** Cópia defensiva: quem chama não deve alterar o estado interno por acidente. */
    public List<Transacao> todas() {
        return new ArrayList<>(transacoes);
    }

    public ApiResponse<List<Transacao>> listarTodas() {
        List<Transacao> ordenadas = todas();
        ordenadas.sort(Comparator.comparing(Transacao::getData).reversed());
        return ApiResponse.success("Transações encontradas", ordenadas, ordenadas.size());
    }

    /** Somente transações que falharam ou foram estornadas — alimenta o sino de avisos. */
    public ApiResponse<List<Transacao>> listarComErro() {
        List<Transacao> comProblema = todas().stream()
                .filter(t -> "ERRO".equalsIgnoreCase(t.getStatus())
                        || "ESTORNADA".equalsIgnoreCase(t.getStatus()))
                .sorted(Comparator.comparing(Transacao::getData).reversed())
                .toList();
        return ApiResponse.success("Transações com problema", comProblema, comProblema.size());
    }

    public ApiResponse<Transacao> criar(Transacao novaTransacao) {
        String erro = validar(novaTransacao);
        if (erro != null) {
            return ApiResponse.error(erro);
        }

        normalizar(novaTransacao);
        novaTransacao.setId(nextId.getAndIncrement());
        novaTransacao.setCreatedAt(LocalDateTime.now());
        novaTransacao.setAtualizadoEm(LocalDateTime.now());

        transacoes.add(novaTransacao);
        return ApiResponse.success("Transação criada com sucesso", novaTransacao);
    }

    public Optional<ApiResponse<Transacao>> buscarPorId(Long id) {
        return buscarTransacao(id)
                .map(t -> ApiResponse.success("Transação encontrada", t))
                .or(() -> Optional.of(ApiResponse.error("Transação de ID " + id + " não encontrada")));
    }

    public Optional<ApiResponse<Transacao>> atualizar(Long id, Transacao atualizada) {
        return buscarTransacao(id).map(t -> {
            // Só campos presentes mudam: dá para editar só a descrição sem
            // reenviar valor, data e método.
            copiarSePresente(atualizada.getDescricao(), t::setDescricao);
            copiarSePresente(atualizada.getEstabelecimento(), t::setEstabelecimento);
            copiarSePresente(atualizada.getCategoria(), t::setCategoria);
            copiarSePresente(atualizada.getMetodo(), t::setMetodo);
            copiarSePresente(atualizada.getTipo(), t::setTipo);
            copiarSePresente(atualizada.getStatus(), t::setStatus);
            copiarSePresente(atualizada.getMensagemErro(), t::setMensagemErro);
            if (atualizada.getValor() != null && atualizada.getValor() > 0) {
                t.setValor(atualizada.getValor());
            }
            if (atualizada.getData() != null) {
                t.setData(atualizada.getData());
            }
            if (atualizada.getAvaliacao() != null) {
                t.setAvaliacao(atualizada.getAvaliacao());
            }
            t.setAtualizadoEm(LocalDateTime.now());
            return ApiResponse.success("Transação atualizada com sucesso", t);
        }).or(() -> Optional.of(ApiResponse.error("Transação de ID " + id + " não encontrada")));
    }

    public Optional<ApiResponse<Transacao>> remover(Long id) {
        return buscarTransacao(id).map(t -> {
            transacoes.remove(t);
            return ApiResponse.success("Transação removida com sucesso", t);
        }).or(() -> Optional.of(ApiResponse.error("Transação de ID " + id + " não encontrada")));
    }

    // ------------------------------------------------------------------
    // Avaliações
    // ------------------------------------------------------------------

    /** Nota de 1 a 5 para uma transação. */
    public Optional<ApiResponse<Transacao>> avaliar(Long id, Integer nota, String comentario) {
        if (nota == null || nota < 1 || nota > 5) {
            return Optional.of(ApiResponse.error("A nota deve estar entre 1 e 5"));
        }
        return buscarTransacao(id).map(t -> {
            t.setAvaliacao(nota);
            t.setComentarioAvaliacao(comentario);
            t.setAtualizadoEm(LocalDateTime.now());
            return ApiResponse.success("Avaliação registrada", t);
        }).or(() -> Optional.of(ApiResponse.error("Transação de ID " + id + " não encontrada")));
    }

    /**
     * Média das notas de um estabelecimento — o que a tela "avaliar lojas"
     * mostra. Normaliza por caixa para `Netflix` e `NETFLIX` serem a mesma loja.
     */
    public ApiResponse<AvaliacaoEstabelecimento> avaliarEstabelecimento(String estabelecimento) {
        String chave = normalizarEstabelecimento(estabelecimento);
        List<Transacao> doEstabelecimento = todas().stream()
                .filter(t -> t.getEstabelecimento() != null)
                .filter(t -> chave.equals(normalizarEstabelecimento(t.getEstabelecimento())))
                .toList();

        if (doEstabelecimento.isEmpty()) {
            return ApiResponse.error("Nenhuma transação encontrada para " + estabelecimento);
        }

        List<Integer> notas = doEstabelecimento.stream()
                .map(Transacao::getAvaliacao)
                .filter(nota -> nota != null)
                .toList();

        double media = notas.isEmpty()
                ? 0
                : notas.stream().mapToInt(Integer::intValue).average().orElse(0);

        AvaliacaoEstabelecimento avaliacao = new AvaliacaoEstabelecimento();
        avaliacao.setEstabelecimento(chave);
        avaliacao.setNotaMedia(Math.round(media * 10) / 10.0);
        avaliacao.setTotalAvaliacoes(notas.size());
        avaliacao.setTotalTransacoes(doEstabelecimento.size());
        avaliacao.setUltimoComentario(doEstabelecimento.stream()
                .filter(t -> t.getComentarioAvaliacao() != null && !t.getComentarioAvaliacao().isBlank())
                .max(Comparator.comparing(Transacao::getData))
                .map(Transacao::getComentarioAvaliacao)
                .orElse(""));

        return ApiResponse.success("Avaliação do estabelecimento", avaliacao);
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private Optional<Transacao> buscarTransacao(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return transacoes.stream().filter(t -> id.equals(t.getId())).findFirst();
    }

    private String validar(Transacao t) {
        if (t == null) {
            return "Transação é obrigatória";
        }
        if (t.getDescricao() == null || t.getDescricao().isBlank()) {
            return "Descrição é obrigatória";
        }
        if (t.getValor() == null || t.getValor() <= 0) {
            return "Valor deve ser maior que zero";
        }
        if (t.getData() == null) {
            return "Data é obrigatória";
        }
        if (t.getTipo() == null || !(t.getTipo().equalsIgnoreCase("entrada")
                || t.getTipo().equalsIgnoreCase("saida"))) {
            return "Tipo deve ser 'entrada' ou 'saida'";
        }
        if (t.getAvaliacao() != null && (t.getAvaliacao() < 1 || t.getAvaliacao() > 5)) {
            return "A nota deve estar entre 1 e 5";
        }
        return null;
    }

    /** Aplica os padrões para o dado chegar consistente na análise. */
    private void normalizar(Transacao t) {
        t.setTipo(t.getTipo().trim().toLowerCase(Locale.of("pt", "BR")));
        t.setDescricao(t.getDescricao().trim());
        t.setEstabelecimento(vazioParaNull(t.getEstabelecimento()) == null
                ? null
                : t.getEstabelecimento().trim().toUpperCase(Locale.of("pt", "BR")));
        t.setCategoria(t.getCategoria() == null || t.getCategoria().isBlank()
                ? "outros"
                : t.getCategoria().trim().toLowerCase(Locale.of("pt", "BR")));
        t.setMetodo(t.getMetodo() == null || t.getMetodo().isBlank()
                ? "PIX"
                : t.getMetodo().trim().toUpperCase(Locale.of("pt", "BR")));
        t.setStatus(t.getStatus() == null || t.getStatus().isBlank()
                ? "CONCLUIDA"
                : t.getStatus().trim().toUpperCase(Locale.of("pt", "BR")));
    }

    private static String vazioParaNull(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    private static void copiarSePresente(String valor, java.util.function.Consumer<String> destino) {
        if (valor != null && !valor.isBlank()) {
            destino.accept(valor.trim());
        }
    }

    private static String normalizarEstabelecimento(String estabelecimento) {
        return estabelecimento == null
                ? ""
                : estabelecimento.trim().toUpperCase(Locale.of("pt", "BR"));
    }

    // ------------------------------------------------------------------
    // Dados de exemplo
    // ------------------------------------------------------------------

    private void popularDadosDeExemplo() {
        // ---- 2026-07 ----
        adicionar("Salário mensal", "Empresa Exemplo Ltda", "salario", "TED",
                "entrada", 7500.00, LocalDate.of(2026, 7, 5), "CONCLUIDA", null);
        adicionar("Assinatura Netflix", "Netflix", "assinaturas", "CREDITO",
                "saida", 55.90, LocalDate.of(2026, 7, 8), "CONCLUIDA", null);
        adicionar("Plano de música", "Spotify", "assinaturas", "CREDITO",
                "saida", 21.90, LocalDate.of(2026, 7, 10), "CONCLUIDA", null);
        adicionar("Academia", "Smart Fit", "saude", "DEBITO",
                "saida", 109.90, LocalDate.of(2026, 7, 12), "CONCLUIDA", null);
        adicionar("Conta de luz", "Energisa", "moradia", "BOLETO",
                "saida", 187.34, LocalDate.of(2026, 7, 15), "CONCLUIDA", null);
        adicionar("Compra do mês", "Pão de Açúcar", "alimentacao", "DEBITO",
                "saida", 612.45, LocalDate.of(2026, 7, 18), "CONCLUIDA", null);
        adicionar("Corridas Uber", "Uber", "transporte", "PIX",
                "saida", 148.20, LocalDate.of(2026, 7, 22), "CONCLUIDA", null);
        adicionar("Posto Ipiranga", "Posto Ipiranga", "transporte", "CREDITO",
                "saida", 220.00, LocalDate.of(2026, 7, 27), "CONCLUIDA", null);

        // ---- 2026-08 ----
        adicionar("Salário mensal", "Empresa Exemplo Ltda", "salario", "TED",
                "entrada", 7500.00, LocalDate.of(2026, 8, 5), "CONCLUIDA", null);
        adicionar("Assinatura Netflix", "Netflix", "assinaturas", "CREDITO",
                "saida", 55.90, LocalDate.of(2026, 8, 8), "CONCLUIDA", null);
        adicionar("Plano de música", "Spotify", "assinaturas", "CREDITO",
                "saida", 21.90, LocalDate.of(2026, 8, 10), "CONCLUIDA", null);
        adicionar("Academia", "Smart Fit", "saude", "DEBITO",
                "saida", 109.90, LocalDate.of(2026, 8, 12), "CONCLUIDA", null);
        adicionar("Conta de luz", "Energisa", "moradia", "BOLETO",
                "saida", 203.11, LocalDate.of(2026, 8, 15), "CONCLUIDA", null);
        adicionar("Compra do mês", "Pão de Açúcar", "alimentacao", "DEBITO",
                "saida", 588.90, LocalDate.of(2026, 8, 19), "CONCLUIDA", null);
        adicionar("Corridas Uber", "Uber", "transporte", "PIX",
                "saida", 176.55, LocalDate.of(2026, 8, 23), "CONCLUIDA", null);
        adicionar("Pix para Pedro", "Pedro", "transferencias", "PIX",
                "saida", 300.00, LocalDate.of(2026, 8, 25), "ERRO",
                "Saldo insuficiente na conta de origem");

        // ---- 2026-09 ----
        adicionar("Salário mensal", "Empresa Exemplo Ltda", "salario", "TED",
                "entrada", 7800.00, LocalDate.of(2026, 9, 5), "CONCLUIDA", null);
        adicionar("Assinatura Netflix", "Netflix", "assinaturas", "CREDITO",
                "saida", 55.90, LocalDate.of(2026, 9, 8), "CONCLUIDA", null);
        adicionar("Plano de música", "Spotify", "assinaturas", "CREDITO",
                "saida", 21.90, LocalDate.of(2026, 9, 10), "CONCLUIDA", null);
        adicionar("Academia", "Smart Fit", "saude", "DEBITO",
                "saida", 109.90, LocalDate.of(2026, 9, 12), "CONCLUIDA", null);
        adicionar("Conta de luz", "Energisa", "moradia", "BOLETO",
                "saida", 231.87, LocalDate.of(2026, 9, 15), "CONCLUIDA", null);
        adicionar("Compra do mês", "Pão de Açúcar", "alimentacao", "DEBITO",
                "saida", 734.10, LocalDate.of(2026, 9, 19), "CONCLUIDA", null);
        adicionar("Corridas Uber", "Uber", "transporte", "PIX",
                "saida", 214.70, LocalDate.of(2026, 9, 24), "CONCLUIDA", null);
        adicionar("Cinema", "Cinemark", "lazer", "CREDITO",
                "saida", 68.00, LocalDate.of(2026, 9, 28), "CONCLUIDA", null);

        // Algumas já vêm avaliadas para a nota média das lojas não nascer vazia.
        avaliar(2L, 5, "Bom catálogo, preço justo");
        avaliar(4L, 4, "Sempre funciona");
        avaliar(7L, 2, "Caro demais para o trajeto curto");
    }

    private void adicionar(String descricao, String estabelecimento, String categoria,
                           String metodo, String tipo, double valor, LocalDate data,
                           String status, String mensagemErro) {
        Transacao t = new Transacao();
        t.setId(nextId.getAndIncrement());
        t.setDescricao(descricao);
        t.setEstabelecimento(estabelecimento);
        t.setCategoria(categoria);
        t.setMetodo(metodo);
        t.setTipo(tipo);
        t.setValor(valor);
        t.setData(data);
        t.setStatus(status);
        t.setMensagemErro(mensagemErro);
        t.setCreatedAt(LocalDateTime.now());
        transacoes.add(t);
    }
}
