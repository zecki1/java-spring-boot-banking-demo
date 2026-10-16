package com.banking.demo.service;

import com.banking.demo.model.ApiResponse;
import com.banking.demo.model.AvaliacaoEstabelecimento;
import com.banking.demo.model.Transacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TransacaoServiceTest {

    private TransacaoService service;

    @BeforeEach
    void setUp() {
        service = new TransacaoService();
    }

    @Test
    void listarTodas_devolveExemplosDoMaisRecenteParaOMaisAntigo() {
        List<Transacao> transacoes = service.listarTodas().getData();

        assertFalse(transacoes.isEmpty());
        for (int i = 1; i < transacoes.size(); i++) {
            assertFalse(transacoes.get(i - 1).getData().isBefore(transacoes.get(i).getData()),
                    "transações fora de ordem decrescente");
        }
    }

    @Test
    void criar_comDadosValidos_preencheIdStatusEMetodoPadrao() {
        Transacao nova = new Transacao();
        nova.setDescricao("Almoço");
        nova.setEstabelecimento("restaurante b");
        nova.setCategoria("alimentacao");
        nova.setTipo("saida");
        nova.setValor(42.5);
        nova.setData(LocalDate.of(2026, 9, 30));

        ApiResponse<Transacao> response = service.criar(nova);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData().getId());
        assertEquals("CONCLUIDA", response.getData().getStatus());
        // Sem método informado, assume PIX (mais comum no Brasil).
        assertEquals("PIX", response.getData().getMetodo());
        // Estabelecimento padronizado em caixa alta para a análise agrupar bem.
        assertEquals("RESTAURANTE B", response.getData().getEstabelecimento());
    }

    @Test
    void criar_semDescricao_rejeita() {
        Transacao nova = new Transacao();
        nova.setValor(10.0);
        nova.setData(LocalDate.of(2026, 9, 30));
        nova.setTipo("saida");

        assertFalse(service.criar(nova).isSuccess());
    }

    @Test
    void criar_comTipoInvalido_rejeita() {
        Transacao nova = new Transacao();
        nova.setDescricao("Pix quebrado");
        nova.setValor(10.0);
        nova.setData(LocalDate.of(2026, 9, 30));
        nova.setTipo("investimento");

        ApiResponse<Transacao> response = service.criar(nova);

        assertFalse(response.isSuccess());
        assertTrue(response.getMensagem().contains("entrada"));
    }

    @Test
    void criar_comAvaliacaoForaDoIntervalo_rejeita() {
        Transacao nova = new Transacao();
        nova.setDescricao("Teste");
        nova.setValor(10.0);
        nova.setData(LocalDate.of(2026, 9, 30));
        nova.setTipo("saida");
        nova.setAvaliacao(9);

        assertFalse(service.criar(nova).isSuccess());
    }

    @Test
    void atualizar_editaSomenteOsCamposEnviados() {
        Transacao parcial = new Transacao();
        parcial.setDescricao("Descrição corrigida");

        service.atualizar(2L, parcial);

        Transacao atual = service.buscarPorId(2L).orElseThrow().getData();
        assertEquals("Descrição corrigida", atual.getDescricao());
        // Valor e data originais ficam intactos.
        assertEquals(55.90, atual.getValor());
        assertEquals(LocalDate.of(2026, 7, 8), atual.getData());
    }

    @Test
    void atualizar_inexistente_devolveErro() {
        Optional<ApiResponse<Transacao>> response = service.atualizar(9999L, new Transacao());

        assertTrue(response.isPresent());
        assertFalse(response.get().isSuccess());
    }

    @Test
    void avaliar_notaValida_registraNotaEComentario() {
        service.avaliar(2L, 1, "Péssimo atendimento");

        Transacao t = service.buscarPorId(2L).orElseThrow().getData();
        assertEquals(1, t.getAvaliacao());
        assertEquals("Péssimo atendimento", t.getComentarioAvaliacao());
    }

    @Test
    void avaliar_notaForaDoIntervalo_rejeita() {
        Optional<ApiResponse<Transacao>> response = service.avaliar(2L, 0, null);

        assertTrue(response.isPresent());
        assertFalse(response.get().isSuccess());
        // A nota que já existia não pode ser sobrescrita por um valor inválido.
        assertEquals(5, service.buscarPorId(2L).orElseThrow().getData().getAvaliacao());
    }

    @Test
    void avaliarEstabelecimento_calculaMediaETrasOMaisRecente() {
        ApiResponse<AvaliacaoEstabelecimento> response = service.avaliarEstabelecimento("netflix");

        assertTrue(response.isSuccess());
        assertEquals("NETFLIX", response.getData().getEstabelecimento());
        assertEquals(1, response.getData().getTotalAvaliacoes());
        assertEquals(3, response.getData().getTotalTransacoes());
        assertEquals(5.0, response.getData().getNotaMedia());
        assertTrue(response.getData().getUltimoComentario().contains("catálogo"));
    }

    @Test
    void avaliarEstabelecimento_inexistente_devolveErro() {
        ApiResponse<AvaliacaoEstabelecimento> response = service.avaliarEstabelecimento("Loja Inexistente");

        assertFalse(response.isSuccess());
    }

    @Test
    void listarComErro_devolveApenasReprovadasEEstornadas() {
        List<Transacao> comErro = service.listarComErro().getData();

        assertFalse(comErro.isEmpty());
        assertTrue(comErro.stream().allMatch(
                t -> "ERRO".equalsIgnoreCase(t.getStatus())
                        || "ESTORNADA".equalsIgnoreCase(t.getStatus())));
        // O seed tem um Pix reprovado por saldo insuficiente.
        assertTrue(comErro.stream().anyMatch(t -> t.getMensagemErro() != null));
    }

    @Test
    void remover_existente_devolveRemovida() {
        int antes = service.listarTodas().getData().size();

        assertTrue(service.remover(1L).orElseThrow().isSuccess());
        assertEquals(antes - 1, service.listarTodas().getData().size());
    }

    @Test
    void remover_inexistente_devolveErro() {
        assertFalse(service.remover(9999L).orElseThrow().isSuccess());
    }
}
