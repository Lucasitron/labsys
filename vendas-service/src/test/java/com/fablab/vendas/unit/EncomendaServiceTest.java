package com.fablab.vendas.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.vendas.dto.EncomendaCriadaEvent;
import com.fablab.vendas.dto.EncomendaRequest;
import com.fablab.vendas.dto.KanbanRequest;
import com.fablab.vendas.dto.ProducaoStatusEvent;
import com.fablab.vendas.entity.Cliente;
import com.fablab.vendas.entity.Encomenda;
import com.fablab.vendas.entity.StatusKanban;
import com.fablab.vendas.entity.StatusOrcamento;
import com.fablab.vendas.entity.TipoPessoa;
import com.fablab.vendas.repository.ClienteRepository;
import com.fablab.vendas.repository.EncomendaRepository;
import com.fablab.vendas.repository.HistoricoStatusEncomendaRepository;
import com.fablab.vendas.repository.OrcamentoRepository;
import com.fablab.vendas.service.EncomendaService;
import com.fablab.vendas.service.VendasEventPublisher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncomendaServiceTest {

    @Mock
    private EncomendaRepository encomendaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private OrcamentoRepository orcamentoRepository;
    @Mock
    private HistoricoStatusEncomendaRepository historicoRepository;
    @Mock
    private VendasEventPublisher eventPublisher;

    private EncomendaService service;

    @BeforeEach
    void setUp() {
        service = new EncomendaService(encomendaRepository, clienteRepository, orcamentoRepository,
                historicoRepository, eventPublisher);
    }

    private Cliente cliente() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setTipoPessoa(TipoPessoa.PF);
        c.setNomeRazaoSocial("Ana");
        return c;
    }

    private Encomenda encomenda(Long id, StatusKanban status) {
        Encomenda e = new Encomenda();
        e.setId(id);
        e.setCliente(cliente());
        e.setDataCriacao(LocalDate.now());
        e.setDataPrevisaoEntrega(LocalDate.now().plusDays(10));
        e.setStatusKanban(status);
        e.setValorFinal(BigDecimal.valueOf(100));
        return e;
    }

    @Test
    void criarPersisteEncomendaEmFilaEPublicaNoRabbit() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente()));
        when(encomendaRepository.save(any(Encomenda.class))).thenAnswer(inv -> {
            Encomenda e = inv.getArgument(0);
            e.setId(5L);
            return e;
        });

        EncomendaRequest request = new EncomendaRequest(null, 1L, LocalDate.now().plusDays(5),
                BigDecimal.valueOf(50), null);
        var response = service.criar(request);

        assertEquals(5L, response.id());
        assertEquals(StatusKanban.FILA, response.statusKanban());
        verify(eventPublisher).publishEncomendaCriada(any(EncomendaCriadaEvent.class));
    }

    @Test
    void criarRejeitaOrcamentoNaoAprovado() {
        var orcamento = new com.fablab.vendas.entity.Orcamento();
        orcamento.setId(2L);
        orcamento.setStatus(StatusOrcamento.PENDENTE);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente()));
        when(orcamentoRepository.findById(2L)).thenReturn(Optional.of(orcamento));

        EncomendaRequest request = new EncomendaRequest(2L, 1L, LocalDate.now(), BigDecimal.TEN, null);
        assertThrows(IllegalArgumentException.class, () -> service.criar(request));
        verify(encomendaRepository, never()).save(any());
    }

    @Test
    void moverKanbanValidoRegistraHistoricoEPublicaEvento() {
        var encomenda = encomenda(1L, StatusKanban.FILA);
        when(encomendaRepository.findById(1L)).thenReturn(Optional.of(encomenda));
        when(encomendaRepository.saveAndFlush(encomenda)).thenReturn(encomenda);

        var response = service.moverKanban(1L, new KanbanRequest(StatusKanban.PRODUCAO, 9L, "Iniciou"));

        assertEquals(StatusKanban.PRODUCAO, response.statusKanban());
        assertEquals(1, encomenda.getHistorico().size());
        verify(eventPublisher).publishKanbanStatus(any());
        verify(eventPublisher, never()).publishEncomendaEntregue(any());
    }

    @Test
    void moverKanbanInvalidoLancaErro() {
        var encomenda = encomenda(1L, StatusKanban.FILA);
        when(encomendaRepository.findById(1L)).thenReturn(Optional.of(encomenda));
        assertThrows(IllegalArgumentException.class,
                () -> service.moverKanban(1L, new KanbanRequest(StatusKanban.PRONTO, 1L, null)));
        verify(encomendaRepository, never()).saveAndFlush(any());
    }

    @Test
    void entregarEncomendaPublicaEventoDeEntrega() {
        var encomenda = encomenda(1L, StatusKanban.PRONTO);
        when(encomendaRepository.findById(1L)).thenReturn(Optional.of(encomenda));
        when(encomendaRepository.saveAndFlush(encomenda)).thenReturn(encomenda);

        var response = service.moverKanban(1L, new KanbanRequest(StatusKanban.ENTREGUE, 1L, null));

        assertEquals(StatusKanban.ENTREGUE, response.statusKanban());
        verify(eventPublisher).publishEncomendaEntregue(any());
    }

    @Test
    void receberAtualizacaoProducaoAtualizaStatus() {
        var encomenda = encomenda(1L, StatusKanban.FILA);
        when(encomendaRepository.findById(1L)).thenReturn(Optional.of(encomenda));
        when(encomendaRepository.save(encomenda)).thenReturn(encomenda);

        var event = new ProducaoStatusEvent(1L, "PRODUCAO", 1L, "Iniciada");
        var response = service.receberAtualizacaoProducao(event);

        assertEquals(StatusKanban.PRODUCAO, response.statusKanban());
        verify(eventPublisher).publishKanbanStatus(any());
    }

    @Test
    void alterarEscopoEncerraAtualECriaNovaOrdem() {
        var atual = encomenda(1L, StatusKanban.PRODUCAO);
        when(encomendaRepository.findById(1L)).thenReturn(Optional.of(atual));
        when(encomendaRepository.save(any(Encomenda.class))).thenAnswer(inv -> {
            Encomenda e = inv.getArgument(0);
            e.setId(2L);
            return e;
        });

        EncomendaRequest request = new EncomendaRequest(null, 1L, LocalDate.now().plusDays(20),
                BigDecimal.valueOf(200), "novo escopo");
        var response = service.alterarEscopo(1L, request);

        assertEquals(2L, response.id());
        assertEquals(StatusKanban.FILA, response.statusKanban());
        assertEquals(StatusKanban.ENCERRADA, atual.getStatusKanban());
        verify(eventPublisher).publishEncomendaCriada(any(EncomendaCriadaEvent.class));
    }

    @Test
    void alterarEscopoRejeitaEncomendaEntregue() {
        var entregue = encomenda(1L, StatusKanban.ENTREGUE);
        when(encomendaRepository.findById(1L)).thenReturn(Optional.of(entregue));
        EncomendaRequest request = new EncomendaRequest(null, 1L, LocalDate.now(), null, null);
        assertThrows(IllegalArgumentException.class, () -> service.alterarEscopo(1L, request));
    }

    @Test
    void alterarEscopoRejeitaEncomendaJaEncerrada() {
        var encerrada = encomenda(1L, StatusKanban.ENCERRADA);
        when(encomendaRepository.findById(1L)).thenReturn(Optional.of(encerrada));
        EncomendaRequest request = new EncomendaRequest(null, 1L, LocalDate.now(), null, null);
        assertThrows(IllegalArgumentException.class, () -> service.alterarEscopo(1L, request));
    }

    @Test
    void listarPorClienteRetornaEncomendas() {
        when(encomendaRepository.findByCliente_Id(1L)).thenReturn(List.of(encomenda(1L, StatusKanban.FILA)));

        var responses = service.listar(null, 1L, null, null);

        assertEquals(1, responses.size());
        assertTrue(responses.get(0).id() == 1L);
    }

    @Test
    void receberAtualizacaoProducaoInvalidaLancaErro() {
        var encomenda = encomenda(1L, StatusKanban.FILA);
        when(encomendaRepository.findById(1L)).thenReturn(Optional.of(encomenda));

        var event = new ProducaoStatusEvent(1L, "ENTREGUE", 1L, null);
        assertThrows(IllegalArgumentException.class, () -> service.receberAtualizacaoProducao(event));
    }

    @Test
    void moverKanbanIgualLancaErro() {
        var encomenda = encomenda(1L, StatusKanban.FILA);
        when(encomendaRepository.findById(1L)).thenReturn(Optional.of(encomenda));
        assertThrows(IllegalArgumentException.class,
                () -> service.moverKanban(1L, new KanbanRequest(StatusKanban.FILA, 1L, null)));
    }
}