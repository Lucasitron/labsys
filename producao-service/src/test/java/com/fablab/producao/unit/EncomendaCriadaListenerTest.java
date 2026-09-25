package com.fablab.producao.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.producao.dto.EncomendaCriadaEvent;
import com.fablab.producao.entity.EncomendaKanban;
import com.fablab.producao.entity.KanbanStatus;
import com.fablab.producao.repository.EncomendaKanbanRepository;
import com.fablab.producao.consumer.EncomendaCriadaListener;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncomendaCriadaListenerTest {

    @Mock
    private EncomendaKanbanRepository kanbanRepository;

    @InjectMocks
    private EncomendaCriadaListener listener;

    @Test
    void insereEncomendaNaFila() {
        when(kanbanRepository.existsByIdEncomenda(100L)).thenReturn(false);

        listener.onEncomendaCriada(new EncomendaCriadaEvent(100L, 1L, "FILA", BigDecimal.TEN, LocalDate.now()));

        ArgumentCaptor<EncomendaKanban> captor = ArgumentCaptor.forClass(EncomendaKanban.class);
        verify(kanbanRepository).save(captor.capture());
        assertEquals(KanbanStatus.FILA, captor.getValue().getStatus());
        assertEquals(100L, captor.getValue().getIdEncomenda());
    }

    @Test
    void ignoraEncomendaJaExistente() {
        when(kanbanRepository.existsByIdEncomenda(100L)).thenReturn(true);

        listener.onEncomendaCriada(new EncomendaCriadaEvent(100L, 1L, "FILA", BigDecimal.TEN, LocalDate.now()));

        verify(kanbanRepository, never()).save(any());
    }

    @Test
    void ignoraEventoSemIdEncomenda() {
        listener.onEncomendaCriada(new EncomendaCriadaEvent(null, 1L, "FILA", BigDecimal.TEN, LocalDate.now()));

        verify(kanbanRepository, never()).save(any());
    }
}