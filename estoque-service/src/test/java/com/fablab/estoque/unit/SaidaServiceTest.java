package com.fablab.estoque.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.estoque.dto.SaidaRequest;
import com.fablab.estoque.dto.SaidaResponse;
import com.fablab.estoque.entity.Item;
import com.fablab.estoque.entity.SaidaEstoque;
import com.fablab.estoque.entity.TipoSaida;
import com.fablab.estoque.exception.SaldoInsuficienteException;
import com.fablab.estoque.rabbit.EstoqueEventPublisher;
import com.fablab.estoque.repository.ItemRepository;
import com.fablab.estoque.repository.SaidaEstoqueRepository;
import com.fablab.estoque.service.ItemService;
import com.fablab.estoque.service.SaidaService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaidaServiceTest {

    @Mock
    private SaidaEstoqueRepository saidaEstoqueRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private ItemService itemService;
    @Mock
    private EstoqueEventPublisher eventPublisher;

    private SaidaService saidaService;

    @BeforeEach
    void setup() {
        saidaService = new SaidaService(saidaEstoqueRepository, itemRepository, itemService);
    }

    private Item buildItem(Long id, BigDecimal qtd, BigDecimal min) {
        Item item = new Item();
        item.setId(id);
        item.setNome("Resistor");
        item.setQuantidadeAtual(qtd);
        item.setEstoqueMinimo(min);
        return item;
    }

    @Test
    void registrarSaidaDiminuiSaldo() {
        Item item = buildItem(1L, BigDecimal.TEN, BigDecimal.ONE);
        when(itemRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item));
        when(saidaEstoqueRepository.save(any())).thenAnswer(inv -> {
            SaidaEstoque s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SaidaRequest req = new SaidaRequest(1L, BigDecimal.valueOf(3), TipoSaida.CONSUMO, null, null);
        SaidaResponse resp = saidaService.registrar(req);

        assertNotNull(resp);
        assertEquals(0, new BigDecimal("7.00").compareTo(item.getQuantidadeAtual()));
        verify(itemService).verificarEstoqueBaixo(item);
    }

    @Test
    void registrarSaidaInsuficienteLancaSaldoInsuficiente() {
        Item item = buildItem(1L, BigDecimal.valueOf(5), BigDecimal.ONE);
        when(itemRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item));

        SaidaRequest req = new SaidaRequest(1L, BigDecimal.valueOf(20), TipoSaida.CONSUMO, null, null);
        assertThrows(SaldoInsuficienteException.class, () -> saidaService.registrar(req));
    }

    @Test
    void registrarSaidaNaoPermiteEstoqueNegativo() {
        Item item = buildItem(1L, BigDecimal.valueOf(2), BigDecimal.ONE);
        when(itemRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item));

        SaidaRequest req = new SaidaRequest(1L, BigDecimal.valueOf(3), TipoSaida.PERDA, null, null);
        assertThrows(SaldoInsuficienteException.class, () -> saidaService.registrar(req));
    }

    @Test
    void listarPorItemRetornaHistorico() {
        SaidaEstoque saida = new SaidaEstoque();
        saida.setId(1L);
        Item item = buildItem(1L, BigDecimal.TEN, BigDecimal.ONE);
        saida.setItem(item);
        saida.setQuantidade(BigDecimal.ONE);
        saida.setTipoSaida(TipoSaida.CONSUMO);
        saida.setDataSaida(java.time.LocalDateTime.now());
        when(saidaEstoqueRepository.findByItemId(1L)).thenReturn(List.of(saida));

        assertEquals(1, saidaService.listarPorItem(1L).size());
    }
}