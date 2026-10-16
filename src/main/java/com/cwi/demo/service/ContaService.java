package com.cwi.demo.service;

import com.cwi.demo.model.Conta;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service para lógica de negócio de contas bancárias
 * 
 * Em produção, este service teria:
 * - @Transactional para garantir atomicidade de transferências
 * - Chamadas a um Repository (JPA/Hibernate) para persistência
 * - Integração com core bancário real
 * - Validações mais complexas (limite diário, blacklist, etc)
 */
@Service
public class ContaService {

    // Banco de dados em memória (simulando um Repository JPA)
    private final List<Conta> contas = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public ContaService() {
        // Dados de teste (simulando contas reais)
        contas.add(new Conta(1L, "0001-1", "João Silva", "123.456.789-00", 
                new BigDecimal("5000.00"), "0001"));
        contas.add(new Conta(2L, "0001-2", "Maria Santos", "987.654.321-00", 
                new BigDecimal("3500.00"), "0001"));
        contas.add(new Conta(3L, "0002-1", "Pedro Oliveira", "456.789.123-00", 
                new BigDecimal("10000.00"), "0002"));
        nextId.set(4L);
    }

    /**
     * Lista todas as contas cadastradas
     */
    public List<Conta> listarTodas() {
        return new ArrayList<>(contas);
    }

    /**
     * Busca uma conta pelo ID
     */
    public Optional<Conta> buscarPorId(Long id) {
        return contas.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst();
    }

    /**
     * Consulta o saldo de uma conta
     */
    public Optional<BigDecimal> consultarSaldo(Long id) {
        return buscarPorId(id).map(Conta::getSaldo);
    }

    /**
     * Cria uma nova conta bancária
     * Em produção, isso seria @Transactional e usaria um Repository
     */
    public Conta criar(Conta novaConta) {
        novaConta.setId(nextId.getAndIncrement());
        novaConta.setSaldo(novaConta.getSaldoInicial());
        contas.add(novaConta);
        return novaConta;
    }

    /**
     * Realiza transferência entre duas contas
     * 
     * EM PRODUÇÃO, este método teria:
     * @Transactional
     * public Optional<Conta> transferir(Long origem, Long destino, BigDecimal valor) {
     *     // Se qualquer operação falhar, rollback automático
     *     // 1. Debitar da conta origem
     *     // 2. Creditar na conta destino
     *     // 3. Registrar transação no log
     *     // 4. Enviar notificação
     *     // Se 2, 3 ou 4 falhar → rollback da operação 1
     * }
     * 
     * Sem @Transactional, se algo falhar no meio, pode debit sem creditar!
     * Isso é CRÍTICO em sistemas bancários
     */
    public Optional<Conta> transferir(Long contaOrigemId, Long contaDestinoId, BigDecimal valor) {
        // Busca as duas contas
        Optional<Conta> origemOpt = buscarPorId(contaOrigemId);
        Optional<Conta> destinoOpt = buscarPorId(contaDestinoId);

        if (origemOpt.isEmpty() || destinoOpt.isEmpty()) {
            return Optional.empty();
        }

        Conta origem = origemOpt.get();
        Conta destino = destinoOpt.get();

        // Valida saldo suficiente
        if (origem.getSaldo().compareTo(valor) < 0) {
            throw new RuntimeException("Saldo insuficiente na conta de origem");
        }

        // Realiza a transferência
        // Em produção, isso seria atômico com @Transactional
        origem.setSaldo(origem.getSaldo().subtract(valor));
        destino.setSaldo(destino.getSaldo().add(valor));

        // Retorna a conta de origem atualizada
        return Optional.of(origem);
    }
}