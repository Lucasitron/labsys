package com.fablab.financeiro.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.fablab.financeiro.consumer.ProducaoConcluidaListener;
import com.fablab.financeiro.dto.ProducaoConcluidaEvent;
import com.fablab.financeiro.service.CustoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProducaoConcluidaListenerTest {

    @Mock
    private CustoService custoService;

    @InjectMocks
    private ProducaoConcluidaListener listener;

    @Test
    void calculaCustoAoConcluirProducao() {
        listener.aoConcluirProducao(new ProducaoConcluidaEvent(1L, 2L, List.of()));
        verify(custoService).calcularCusto(1L);
    }

    @Test
    void falhaNaoPropagaExcecao() {
        doThrow(new RuntimeException("erro")).when(custoService).calcularCusto(any());
        var event = new ProducaoConcluidaEvent(1L, 2L, List.of());
        assertDoesNotThrow(() -> listener.aoConcluirProducao(event));
    }
}