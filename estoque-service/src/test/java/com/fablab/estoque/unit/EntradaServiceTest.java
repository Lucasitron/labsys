package com.fablab.estoque.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.estoque.dto.EntradaRequest;
import com.fablab.estoque.dto.EntradaResponse;
import com.fablab.estoque.entity.EntradaEstoque;
import com.fablab.estoque.entity.Fornecedor;
import com.fablab.estoque.entity.Item;
import com.fablab.estoque.exception.ResourceNotFoundException;
import com.fablab.estoque.rabbit.EstoqueEventPublisher;
import com.fablab.estoque.repository.EntradaEstoqueRepository;
import com.fablab.estoque.repository.FornecedorRepository;
import com.fablab.estoque.repository.ItemRepository;
import com.fablab.estoque.service.EntradaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntradaServiceTest {

    @Mock
    private EntradaEstoqueRepository entradaEstoqueRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private FornecedorRepository fornecedorRepository;
    @Mock
    private EstoqueEventPublisher eventPublisher;

    private EntradaService entradaService;

    @BeforeEach
    void setup() {
        entradaService = new EntradaService(
                entradaEstoqueRepository, itemRepository, fornecedorRepository, eventPublisher);
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
    void registrarEntradaAumentaSaldoCalculaValorTotalEPublicaCompra() {
        Item item = buildItem(1L, BigDecimal.TEN, BigDecimal.valueOf(50));
        when(itemRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item));
        Fornecedor forn = new Fornecedor();
        forn.setId(1L);
        forn.setNome("Digikey");
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(forn));
        when(entradaEstoqueRepository.save(any())).thenAnswer(inv -> {
            EntradaEstoque e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EntradaRequest req = new EntradaRequest(1L, 1L, BigDecimal.valueOf(30), new BigDecimal("2.50"),
                LocalDate.now(), "NF-001", null);
        EntradaResponse resp = entradaService.registrar(req);

        assertNotNull(resp);
        assertEquals(0, new BigDecimal("40.00").compareTo(item.getQuantidadeAtual()));
        assertEquals(0, new BigDecimal("75.00").compareTo(resp.valorTotal()));
        verify(eventPublisher).publishCompraSolicitada(any(EntradaEstoque.class));
        verify(eventPublisher).publishEstoqueBaixo(item);
    }

    @Test
    void registrarEntradaSemFornecedorLancaExcecao() {
        Item item = buildItem(1L, BigDecimal.TEN, BigDecimal.ONE);
        when(itemRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item));
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.empty());

        EntradaRequest req = new EntradaRequest(1L, 1L, BigDecimal.ONE, BigDecimal.ONE, null, null, null);
        assertThrows(ResourceNotFoundException.class, () -> entradaService.registrar(req));
    }
}