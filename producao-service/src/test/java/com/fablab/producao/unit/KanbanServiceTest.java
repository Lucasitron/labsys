package com.fablab.producao.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.producao.dto.KanbanMovimentoRequest;
import com.fablab.producao.dto.KanbanRequest;
import com.fablab.producao.dto.KanbanStatusAlteradoEvent;
import com.fablab.producao.dto.ProducaoConcluidaEvent;
import com.fablab.producao.dto.ProducaoStatusEvent;
import com.fablab.producao.entity.ConsumoEncomenda;
import com.fablab.producao.entity.EncomendaKanban;
import com.fablab.producao.entity.KanbanStatus;
import com.fablab.producao.exception.ForbiddenException;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.ConsumoEncomendaRepository;
import com.fablab.producao.repository.EncomendaKanbanRepository;
import com.fablab.producao.repository.HistoricoKanbanRepository;
import com.fablab.producao.service.AcessoService;
import com.fablab.producao.service.KanbanService;
import com.fablab.producao.service.ProducaoEventPublisher;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KanbanServiceTest {

    @Mock
    private EncomendaKanbanRepository kanbanRepository;
    @Mock
    private HistoricoKanbanRepository historicoRepository;
    @Mock
    private ConsumoEncomendaRepository consumoRepository;
    @Mock
    private ProducaoEventPublisher eventPublisher;
    @Mock
    private AcessoService acessoService;

    @InjectMocks
    private KanbanService service;

    private EncomendaKanban kanban(Long id, Long idEncomenda, KanbanStatus status, Long idResponsavel) {
        EncomendaKanban k = new EncomendaKanban();
        k.setIdKanban(id);
        k.setIdEncomenda(idEncomenda);
        k.setStatus(status);
        k.setIdResponsavel(idResponsavel);
        k.setOrdem(0);
        return k;
    }

    @Test
    void listarPorStatus() {
        when(kanbanRepository.findByStatusOrderByOrdemAsc(KanbanStatus.FILA))
                .thenReturn(List.of(kanban(1L, 100L, KanbanStatus.FILA, 5L)));
        assertEquals(1, service.listar(KanbanStatus.FILA).size());
    }

    @Test
    void listarTodos() {
        when(kanbanRepository.findAllByOrderByStatusAscOrdemAsc())
                .thenReturn(List.of(kanban(1L, 100L, KanbanStatus.FILA, 5L)));
        assertEquals(1, service.listar(null).size());
    }

    @Test
    void buscarPorEncomendaInexistenteLancaExcecao() {
        when(kanbanRepository.findByIdEncomenda(100L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.buscarPorEncomenda(100L));
    }

    @Test
    void historicoRetornaLista() {
        when(historicoRepository.findByIdEncomendaOrderByDataAlteracaoDesc(100L)).thenReturn(List.of());
        assertEquals(0, service.historico(100L).size());
    }

    @Test
    void incluirCriaCartaoNaFilaEHistorico() {
        when(kanbanRepository.existsByIdEncomenda(100L)).thenReturn(false);
        when(kanbanRepository.save(any(EncomendaKanban.class))).thenAnswer(inv -> {
            EncomendaKanban k = inv.getArgument(0);
            k.setIdKanban(1L);
            return k;
        });

        var response = service.incluir(new KanbanRequest(100L, 5L, 2));

        assertEquals(KanbanStatus.FILA, response.status());
        assertEquals(1L, response.idKanban());
        verify(historicoRepository).save(any());
    }

    @Test
    void incluirEncomendaDuplicadaFalha() {
        when(kanbanRepository.existsByIdEncomenda(100L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.incluir(new KanbanRequest(100L, 5L, null)));
        verify(kanbanRepository, never()).save(any());
    }

    @Test
    void moverPublicaEventosDeStatus() {
        EncomendaKanban kanban = kanban(1L, 100L, KanbanStatus.FILA, 5L);
        when(kanbanRepository.findById(1L)).thenReturn(Optional.of(kanban));
        when(acessoService.podeEditar(5L)).thenReturn(true);

        service.mover(1L, new KanbanMovimentoRequest(KanbanStatus.PRODUCAO, 7L, "iniciando"));

        verify(eventPublisher).publicarStatusAlterado(any(ProducaoStatusEvent.class));
        verify(eventPublisher).publicarKanbanStatusAlterado(any(KanbanStatusAlteradoEvent.class));
    }

    @Test
    void moverParaEntreguePublicaConclusaoComConsumo() {
        EncomendaKanban kanban = kanban(1L, 100L, KanbanStatus.PRONTO, 5L);
        when(kanbanRepository.findById(1L)).thenReturn(Optional.of(kanban));
        when(acessoService.podeEditar(5L)).thenReturn(true);
        ConsumoEncomenda consumo = new ConsumoEncomenda();
        consumo.setIdEncomenda(100L);
        consumo.setIdItem(50L);
        consumo.setQuantidadeConsumida(new BigDecimal("2.50"));
        when(consumoRepository.findByIdEncomenda(100L)).thenReturn(List.of(consumo));

        service.mover(1L, new KanbanMovimentoRequest(KanbanStatus.ENTREGUE, null, null));

        ArgumentCaptor<ProducaoConcluidaEvent> captor = ArgumentCaptor.forClass(ProducaoConcluidaEvent.class);
        verify(eventPublisher).publicarProducaoConcluida(captor.capture());
        assertEquals(100L, captor.getValue().idEncomenda());
        assertEquals(1, captor.getValue().itens().size());
        assertEquals(50L, captor.getValue().itens().get(0).idItem());
    }

    @Test
    void moverSemPermissaoEhProibido() {
        when(kanbanRepository.findById(1L)).thenReturn(Optional.of(kanban(1L, 100L, KanbanStatus.FILA, 5L)));
        when(acessoService.podeEditar(5L)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> service.mover(1L, new KanbanMovimentoRequest(KanbanStatus.PRODUCAO, null, null)));
        verify(eventPublisher, never()).publicarStatusAlterado(any());
    }

    @Test
    void removerCartao() {
        EncomendaKanban kanban = kanban(1L, 100L, KanbanStatus.FILA, 5L);
        when(kanbanRepository.findById(1L)).thenReturn(Optional.of(kanban));
        when(acessoService.podeEditar(5L)).thenReturn(true);

        service.remover(1L);

        verify(kanbanRepository).delete(kanban);
    }
}