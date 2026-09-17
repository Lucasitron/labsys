package com.fablab.financeiro.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fablab.financeiro.consumer.HorasValidadasListener;
import com.fablab.financeiro.dto.HorasValidadasEvent;
import com.fablab.financeiro.service.FechamentoEncomendaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HorasValidadasListenerTest {

    @Mock
    private FechamentoEncomendaService fechamentoService;

    @InjectMocks
    private HorasValidadasListener listener;

    @Test
    void acumulaHorasDeEncomenda() {
        var event = new HorasValidadasEvent(5L, "ENCOMENDA", 1L, BigDecimal.valueOf(8), LocalDate.now());
        listener.aoValidarHoras(event);
        verify(fechamentoService).registrarHorasValidadas(
                eq(1L), eq(5L), eq(null), eq(BigDecimal.valueOf(8)), any(LocalDate.class));
    }

    @Test
    void ignoraApontamentosDeProjeto() {
        var event = new HorasValidadasEvent(5L, "PROJETO", 1L, BigDecimal.valueOf(8), LocalDate.now());
        listener.aoValidarHoras(event);
        verify(fechamentoService, never()).registrarHorasValidadas(any(), any(), any(), any(), any());
    }

    @Test
    void falhaNaoPropagaExcecao() {
        doThrow(new RuntimeException("erro")).when(fechamentoService)
                .registrarHorasValidadas(any(), any(), any(), any(), any());
        var event = new HorasValidadasEvent(5L, "ENCOMENDA", 1L, BigDecimal.ONE, LocalDate.now());
        assertDoesNotThrow(() -> listener.aoValidarHoras(event));
    }
}