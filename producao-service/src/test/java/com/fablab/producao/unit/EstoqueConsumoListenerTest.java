package com.fablab.producao.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.producao.dto.EstoqueConsumoEvent;
import com.fablab.producao.entity.ConsumoEncomenda;
import com.fablab.producao.repository.ConsumoEncomendaRepository;
import com.fablab.producao.consumer.EstoqueConsumoListener;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EstoqueConsumoListenerTest {

    @Mock
    private ConsumoEncomendaRepository consumoRepository;

    @InjectMocks
    private EstoqueConsumoListener listener;

    @Test
    void criaConsumoQuandoNaoExiste() {
        when(consumoRepository.findByIdEncomenda(100L)).thenReturn(List.of());

        listener.onConsumoRealizado(new EstoqueConsumoEvent(100L, 50L, new BigDecimal("3.00")));

        ArgumentCaptor<ConsumoEncomenda> captor = ArgumentCaptor.forClass(ConsumoEncomenda.class);
        verify(consumoRepository).save(captor.capture());
        assertEquals(new BigDecimal("3.00"), captor.getValue().getQuantidadeConsumida());
    }

    @Test
    void acumulaConsumoExistente() {
        ConsumoEncomenda existente = new ConsumoEncomenda();
        existente.setIdEncomenda(100L);
        existente.setIdItem(50L);
        existente.setQuantidadeConsumida(new BigDecimal("2.00"));
        when(consumoRepository.findByIdEncomenda(100L)).thenReturn(List.of(existente));

        listener.onConsumoRealizado(new EstoqueConsumoEvent(100L, 50L, new BigDecimal("1.50")));

        assertEquals(new BigDecimal("3.50"), existente.getQuantidadeConsumida());
        verify(consumoRepository).save(existente);
    }

    @Test
    void ignoraEventoIncompleto() {
        listener.onConsumoRealizado(new EstoqueConsumoEvent(100L, null, BigDecimal.ONE));

        verify(consumoRepository, never()).save(any());
    }
}