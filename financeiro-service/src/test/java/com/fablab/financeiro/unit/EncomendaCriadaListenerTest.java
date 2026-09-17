package com.fablab.financeiro.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.fablab.financeiro.consumer.EncomendaCriadaListener;
import com.fablab.financeiro.dto.EncomendaCriadaEvent;
import com.fablab.financeiro.service.FechamentoEncomendaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncomendaCriadaListenerTest {

    @Mock
    private FechamentoEncomendaService fechamentoService;

    @InjectMocks
    private EncomendaCriadaListener listener;

    @Test
    void abreFechamentoAoReceberEvento() {
        var event = new EncomendaCriadaEvent(1L, 2L, "FILA", BigDecimal.TEN, LocalDate.now());
        listener.aoCriarEncomenda(event);
        verify(fechamentoService).criarDoEvento(event);
    }

    @Test
    void falhaNaoPropagaExcecao() {
        doThrow(new RuntimeException("erro")).when(fechamentoService).criarDoEvento(any());
        var event = new EncomendaCriadaEvent(1L, 2L, "FILA", BigDecimal.TEN, LocalDate.now());
        assertDoesNotThrow(() -> listener.aoCriarEncomenda(event));
    }
}