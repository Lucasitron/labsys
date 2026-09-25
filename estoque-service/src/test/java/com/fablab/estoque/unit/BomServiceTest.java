package com.fablab.estoque.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.estoque.dto.BomConsumoItem;
import com.fablab.estoque.dto.BomConsumoRequest;
import com.fablab.estoque.dto.BomItemRequest;
import com.fablab.estoque.dto.BomRequest;
import com.fablab.estoque.dto.BomResponse;
import com.fablab.estoque.entity.Categoria;
import com.fablab.estoque.entity.Item;
import com.fablab.estoque.entity.ItemBom;
import com.fablab.estoque.entity.ListaMateriais;
import com.fablab.estoque.exception.ResourceNotFoundException;
import com.fablab.estoque.repository.ItemRepository;
import com.fablab.estoque.repository.ListaMateriaisRepository;
import com.fablab.estoque.service.BomService;
import com.fablab.estoque.service.ItemService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BomServiceTest {

    @Mock
    private ListaMateriaisRepository bomRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private ItemService itemService;
    @InjectMocks
    private BomService bomService;

    private Item buildItem(Long id, String nome) {
        Item item = new Item();
        item.setId(id);
        item.setNome(nome);
        item.setCategoria(Categoria.INSUMO);
        item.setUnidadeMedida("un");
        item.setQuantidadeAtual(BigDecimal.TEN);
        item.setEstoqueMinimo(BigDecimal.ONE);
        return item;
    }

    private ListaMateriais buildBom(Long id) {
        ListaMateriais bom = new ListaMateriais();
        bom.setId(id);
        bom.setNome("BOM Impressora");
        bom.setIdProdutoServico(1L);
        bom.setVersao(1);
        bom.setEditavel(true);
        return bom;
    }

    @Test
    void criarBomComItens() {
        Item item = buildItem(1L, "Resistor");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(bomRepository.save(any())).thenAnswer(inv -> {
            ListaMateriais b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        BomRequest req = new BomRequest(1L, "BOM Impressora", 1, true,
                List.of(new BomItemRequest(1L, BigDecimal.valueOf(5))));
        BomResponse resp = bomService.criar(req);

        assertNotNull(resp);
        assertEquals(1, resp.itens().size());
    }

    @Test
    void buscarBomNaoEncontrada() {
        when(bomRepository.findWithItensById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bomService.buscar(99L));
    }

    @Test
    void atualizarBomIncrementaVersao() {
        ListaMateriais bom = buildBom(1L);
        Item item = buildItem(1L, "Capacitor");
        ItemBom ib = new ItemBom();
        ib.setId(1L);
        ib.setItem(item);
        ib.setQuantidadePrevista(BigDecimal.TEN);
        bom.adicionarItem(ib);

        when(bomRepository.findWithItensById(1L)).thenReturn(Optional.of(bom));
        when(bomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BomRequest req = new BomRequest(1L, "BOM Atualizada", null, false,
                List.of(new BomItemRequest(1L, BigDecimal.valueOf(20))));
        BomResponse resp = bomService.atualizar(1L, req);

        assertEquals(2, resp.versao());
        assertEquals(false, resp.editavel());
    }

    @Test
    void registrarConsumoItemNaoPertenceBomLancaExcecao() {
        ListaMateriais bom = buildBom(1L);
        Item item = buildItem(1L, "Resistor");
        ItemBom ib = new ItemBom();
        ib.setId(1L);
        ib.setItem(item);
        ib.setQuantidadePrevista(BigDecimal.TEN);
        bom.adicionarItem(ib);

        when(bomRepository.findWithItensById(1L)).thenReturn(Optional.of(bom));

        BomConsumoRequest req = new BomConsumoRequest(List.of(new BomConsumoItem(99L, BigDecimal.ONE)));
        assertThrows(IllegalArgumentException.class, () -> bomService.registrarConsumo(1L, req));
    }

    @Test
    void registrarConsumoOkChamaItemService() {
        ListaMateriais bom = buildBom(1L);
        Item item = buildItem(1L, "Resistor");
        ItemBom ib = new ItemBom();
        ib.setId(1L);
        ib.setItem(item);
        ib.setQuantidadePrevista(BigDecimal.TEN);
        bom.adicionarItem(ib);

        when(bomRepository.findWithItensById(1L)).thenReturn(Optional.of(bom));

        BomConsumoRequest req = new BomConsumoRequest(List.of(new BomConsumoItem(1L, BigDecimal.valueOf(3))));
        BomResponse resp = bomService.registrarConsumo(1L, req);

        assertNotNull(resp);
        assertEquals(new BigDecimal("3"), resp.itens().getFirst().quantidadeReal());
        verify(itemService).registrarConsumo(1L, BigDecimal.valueOf(3), 1L);
    }
}