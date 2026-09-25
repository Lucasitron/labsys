package com.fablab.estoque.messaging;

import static org.mockito.Mockito.atLeastOnce;

import com.fablab.estoque.dto.ItemConsumido;
import com.fablab.estoque.dto.ProducaoConcluidaEvent;
import com.fablab.estoque.entity.Categoria;
import com.fablab.estoque.integration.BaseIntegrationTest;
import com.fablab.estoque.rabbit.ConsumoProducaoListener;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;

class MensageriaIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ConsumoProducaoListener consumoListener;

    @Test
    void consumidorBaixaEstoqueAoReceberEventoProducao() {
        var item1 = seedItem("Resistor", Categoria.INSUMO, BigDecimal.valueOf(100), BigDecimal.TEN);
        var item2 = seedItem("Capacitor", Categoria.PECA, BigDecimal.valueOf(50), BigDecimal.TEN);

        ProducaoConcluidaEvent event = new ProducaoConcluidaEvent(
                100L, 10L,
                List.of(
                        new ItemConsumido(item1.getId(), BigDecimal.valueOf(20)),
                        new ItemConsumido(item2.getId(), BigDecimal.valueOf(10))));

        consumoListener.onProducaoConcluida(event);

        var atualizado1 = itemRepository.findById(item1.getId()).orElseThrow();
        var atualizado2 = itemRepository.findById(item2.getId()).orElseThrow();
        Assertions.assertEquals(0, new BigDecimal("80.00").compareTo(atualizado1.getQuantidadeAtual()));
        Assertions.assertEquals(0, new BigDecimal("40.00").compareTo(atualizado2.getQuantidadeAtual()));
    }

    @Test
    void eventoDuplicadoNaoBaixaDuasVezes() {
        var item1 = seedItem("Resistor", Categoria.INSUMO, BigDecimal.valueOf(100), BigDecimal.TEN);

        ProducaoConcluidaEvent event = new ProducaoConcluidaEvent(
                200L, 10L,
                List.of(new ItemConsumido(item1.getId(), BigDecimal.valueOf(20))));

        consumoListener.onProducaoConcluida(event);
        consumoListener.onProducaoConcluida(event);

        var atualizado = itemRepository.findById(item1.getId()).orElseThrow();
        Assertions.assertEquals(0, new BigDecimal("80.00").compareTo(atualizado.getQuantidadeAtual()));
    }

    @Test
    void publisherEmiteEstoqueBaixoParaBroker() {
        var item = seedItem("Fio", Categoria.INSUMO, BigDecimal.valueOf(3), BigDecimal.TEN);
        itemService.verificarEstoqueBaixo(item);
        org.mockito.Mockito.verify(rabbitTemplate, atLeastOnce())
                .convertAndSend(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(Object.class));
    }
}