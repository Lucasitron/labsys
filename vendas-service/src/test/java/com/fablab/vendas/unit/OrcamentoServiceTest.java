package com.fablab.vendas.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.vendas.dto.EncomendaRequest;
import com.fablab.vendas.dto.ItemOrcamentoRequest;
import com.fablab.vendas.dto.OrcamentoRequest;
import com.fablab.vendas.dto.OrcamentoResponse;
import com.fablab.vendas.dto.OrcamentoStatusRequest;
import com.fablab.vendas.entity.Cliente;
import com.fablab.vendas.entity.Encomenda;
import com.fablab.vendas.entity.Orcamento;
import com.fablab.vendas.entity.StatusKanban;
import com.fablab.vendas.entity.StatusOrcamento;
import com.fablab.vendas.entity.TipoPessoa;
import com.fablab.vendas.repository.ClienteRepository;
import com.fablab.vendas.repository.EncomendaRepository;
import com.fablab.vendas.repository.HistoricoStatusEncomendaRepository;
import com.fablab.vendas.repository.OrcamentoRepository;
import com.fablab.vendas.service.OrcamentoService;
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
class OrcamentoServiceTest {

    @Mock
    private OrcamentoRepository orcamentoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private EncomendaRepository encomendaRepository;
    @Mock
    private HistoricoStatusEncomendaRepository historicoRepository;
    @Mock
    private VendasEventPublisher eventPublisher;

    private OrcamentoService service;

    @BeforeEach
    void setUp() {
        service = new OrcamentoService(orcamentoRepository, clienteRepository, encomendaRepository,
                historicoRepository, eventPublisher);
    }

    private Cliente cliente() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setTipoPessoa(TipoPessoa.PF);
        c.setNomeRazaoSocial("Maria");
        return c;
    }

    private Orcamento orcamento(Long id, StatusOrcamento status) {
        Orcamento o = new Orcamento();
        o.setId(id);
        o.setCliente(cliente());
        o.setDataCriacao(LocalDate.now());
        o.setValidade(LocalDate.now().plusDays(10));
        o.setStatus(status);
        return o;
    }

    @Test
    void criarCalculaValorTotal() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente()));
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(inv -> {
            Orcamento o = inv.getArgument(0);
            o.setId(10L);
            return o;
        });

        OrcamentoRequest request = new OrcamentoRequest(1L, LocalDate.now().plusDays(10), null,
                List.of(new ItemOrcamentoRequest("A", BigDecimal.valueOf(2), BigDecimal.valueOf(10)),
                        new ItemOrcamentoRequest("B", BigDecimal.ONE, BigDecimal.valueOf(5))));

        OrcamentoResponse response = service.criar(request);

        assertEquals(0, BigDecimal.valueOf(25).compareTo(response.valorTotal()));
        assertEquals(StatusOrcamento.PENDENTE, response.status());
    }

    @Test
    void criarClienteInexistenteLancaErro() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());
        OrcamentoRequest request = new OrcamentoRequest(9L, LocalDate.now(), null,
                List.of(new ItemOrcamentoRequest("A", BigDecimal.ONE, BigDecimal.ONE)));
        assertThrows(Exception.class, () -> service.criar(request));
    }

    @Test
    void atualizarRejeitaOrcamentoNaoPendente() {
        var aprovado = orcamento(1L, StatusOrcamento.APROVADO);
        when(orcamentoRepository.findById(1L)).thenReturn(Optional.of(aprovado));
        OrcamentoRequest request = new OrcamentoRequest(1L, LocalDate.now().plusDays(5), null,
                List.of(new ItemOrcamentoRequest("A", BigDecimal.ONE, BigDecimal.ONE)));
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(1L, request));
        verify(orcamentoRepository, never()).save(aprovado);
    }

    @Test
    void aprovarOrcamentoPublicaEvento() {
        var pendente = orcamento(1L, StatusOrcamento.PENDENTE);
        when(orcamentoRepository.findById(1L)).thenReturn(Optional.of(pendente));
        when(orcamentoRepository.save(pendente)).thenReturn(pendente);

        OrcamentoResponse response = service.mudarStatus(1L, new OrcamentoStatusRequest(StatusOrcamento.APROVADO));

        assertEquals(StatusOrcamento.APROVADO, response.status());
        verify(eventPublisher).publishOrcamentoAprovado(any());
    }

    @Test
    void converterParaEncomendaExigeAprovacao() {
        var pendente = orcamento(1L, StatusOrcamento.PENDENTE);
        when(orcamentoRepository.findById(1L)).thenReturn(Optional.of(pendente));
        EncomendaRequest request = new EncomendaRequest(null, 1L, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.converterParaEncomenda(1L, request));
    }

    @Test
    void converterAprovadoCriaEncomendaEPublicaEvento() {
        var aprovado = orcamento(1L, StatusOrcamento.APROVADO);
        aprovado.recalcularValorTotal();
        when(orcamentoRepository.findById(1L)).thenReturn(Optional.of(aprovado));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente()));
        when(encomendaRepository.save(any(Encomenda.class))).thenAnswer(inv -> {
            Encomenda e = inv.getArgument(0);
            e.setId(7L);
            return e;
        });

        EncomendaRequest request = new EncomendaRequest(1L, 1L, LocalDate.now().plusDays(3), null, null);
        var response = service.converterParaEncomenda(1L, request);

        assertEquals(7L, response.id());
        assertEquals(StatusKanban.FILA, response.statusKanban());
        verify(eventPublisher).publishEncomendaCriada(any());
    }
}