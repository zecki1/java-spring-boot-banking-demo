package com.banking.demo.service;

import com.banking.demo.model.Transacao;
import com.banking.demo.model.ApiResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service layer para lógica de negócio
 * Equivalente à lógica que fica no server.js no Express, mas:
 * - Separado em camada própria (Separation of Concerns)
 * - Anotado com @Service para injeção de dependência
 * - Pode ter transações @Transactional (não aplicável aqui sem banco)
 */
@Service
public class TransacaoService {

    // Banco de dados em memória (simulando um repository real)
    private final List<Transacao> transacoes = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1); // Thread-safe counter

    public TransacaoService() {
        // Dados de exemplo
        transacoes.add(new Transacao(1L, "Salário", 3000.00, LocalDate.of(2024, 3, 1), "entrada"));
        transacoes.add(new Transacao(2L, "Aluguel", -1200.00, LocalDate.of(2024, 3, 2), "saida"));
        transacoes.add(new Transacao(3L, "Lanche", -25.50, LocalDate.of(2024, 3, 3), "saida"));
        nextId.set(4L);
    }

    /**
     * GET /api/transacoes
     * Retorna todas as transações ordenadas por data (mais recente primeiro)
     * Equivalente a app.get('/api/transacoes', ...) no Express
     */
    public ApiResponse<List<Transacao>> listarTodas() {
        List<Transacao> sorted = new ArrayList<>(transacoes);
        sorted.sort(Comparator.comparing(Transacao::getData).reversed());
        return ApiResponse.success("Transações encontradas", sorted, sorted.size());
    }

    /**
     * POST /api/transacoes
     * Cria uma nova transação
     * Validações semelhantes às do Express mas em Java forte-typado
     */
    public ApiResponse<Transacao> criar(Transacao novaTransacao) {
        // Validações (em Spring, isso seria @Valid + @NotBlank, @Positive, etc no DTO)
        if (novaTransacao.getDescricao() == null || novaTransacao.getDescricao().isBlank()) {
            return ApiResponse.error("Descrição é obrigatória");
        }
        if (novaTransacao.getValor() == null || novaTransacao.getValor() <= 0) {
            return ApiResponse.error("Valor deve ser maior que zero");
        }
        if (novaTransacao.getData() == null) {
            return ApiResponse.error("Data é obrigatória");
        }

        // Set ID e timestamp
        novaTransacao.setId(nextId.getAndIncrement());
        novaTransacao.setCreatedAt(java.time.LocalDateTime.now());

        transacoes.add(novaTransacao);
        return ApiResponse.success("Transação criada com sucesso", novaTransacao);
    }

    /**
     * GET /api/transacoes/{id}
     * Busca uma transação pelo ID
     * Equivalente a app.get('/api/transacoes/:id', ...) no Express
     * Em Java usamos Optional para tratar ausência de valor
     */
    public Optional<ApiResponse<Transacao>> buscarPorId(Long id) {
        return transacoes.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .map(t -> ApiResponse.success("Transação encontrada", t))
                .or(() -> Optional.of(ApiResponse.error("Transação de ID " + id + " não encontrada")));
    }

    /**
     * PUT /api/transacoes/{id}
     * Atualiza uma transação existente
     * Equivalente a app.put('/api/transacoes/:id', ...) no Express
     */
    public Optional<ApiResponse<Transacao>> atualizar(Long id, Transacao transacaoAtualizada) {
        return transacoes.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .map(t -> {
                    // Atualiza campos (em Spring, isso seria @Transactional com merge)
                    if (transacaoAtualizada.getDescricao() != null && !transacaoAtualizada.getDescricao().isBlank()) {
                        t.setDescricao(transacaoAtualizada.getDescricao());
                    }
                    if (transacaoAtualizada.getValor() != null && transacaoAtualizada.getValor() > 0) {
                        t.setValor(transacaoAtualizada.getValor());
                    }
                    if (transacaoAtualizada.getData() != null) {
                        t.setData(transacaoAtualizada.getData());
                    }
                    if (transacaoAtualizada.getTipo() != null && !transacaoAtualizada.getTipo().isBlank()) {
                        t.setTipo(transacaoAtualizada.getTipo());
                    }
                    return ApiResponse.success("Transação atualizada com sucesso", t);
                })
                .or(() -> Optional.of(ApiResponse.error("Transação de ID " + id + " não encontrada para atualização")));
    }

    /**
     * DELETE /api/transacoes/{id}
     * Remove uma transação
     * Equivalente a app.delete('/api/transacoes/:id', ...) no Express
     */
    public Optional<ApiResponse<Transacao>> remover(Long id) {
        int indexToRemove = -1;
        for (int i = 0; i < transacoes.size(); i++) {
            if (transacoes.get(i).getId().equals(id)) {
                indexToRemove = i;
                break;
            }
        }

        if (indexToRemove == -1) {
            return Optional.of(ApiResponse.error("Transação de ID " + id + " não encontrada para exclusão"));
        }

        Transacao removida = transacoes.remove(indexToRemove);
        return Optional.of(ApiResponse.success("Transação removida com sucesso", removida));
    }
}