package com.fablab.estoque.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.estoque.dto.ItemConsumido;
import com.fablab.estoque.dto.ItemRequest;
import com.fablab.estoque.dto.ItemResponse;
import com.fablab.estoque.dto.SaidaResponse;
import com.fablab.estoque.entity.Categoria;
import com.fablab.estoque.entity.Item;
import com.fablab.estoque.entity.Localizacao;
import com.fablab.estoque.entity.TipoSaida;
import com.fablab.estoque.exception.ResourceNotFoundException;
import com.fablab.estoque.exception.SaldoInsuficienteException;
import com.fablab.estoque.rabbit.EstoqueEventPublisher;
import com.fablab.estoque.repository.ItemRepository;
import com.fablab.estoque.repository.LocalizacaoRepository;
import com.fablab.estoque.repository.SaidaEstoqueRepository;
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
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private LocalizacaoRepository localizacaoRepository;
    @Mock
    private SaidaEstoqueRepository saidaEstoqueRepository;
    @Mock
    private EstoqueEventPublisher eventPublisher;
    @InjectMocks
    private ItemService itemService;

    private Item buildItem(Long id, BigDecimal qtd, BigDecimal min) {
        Item item = new Item();
        item.setId(id);
        item.setNome("Resistor");
        item.setCategoria(Categoria.INSUMO);
        item.setUnidadeMedida("un");
        item.setQuantidadeAtual(qtd);
        item.setEstoqueMinimo(min);
        return item;
    }

    private Item seeded(Localizacao localizacao) {
        Item item = new Item();
        item.setNome("Resistor");
        item.setCategoria(Categoria.INSUMO);
        item.setUnidadeMedida("un");
        item.setQuantidadeAtual(BigDecimal.TEN);
        item.setEstoqueMinimo(BigDecimal.valueOf(5));
        item.setLocalizacao(localizacao);
        return item;
    }

    @Test
    void criarItemPublicaEstoqueBaixoQuandoIgualAoMinimo() {
        ItemRequest req = new ItemRequest("Resistor 10k", null, Categoria.INSUMO, "un", BigDecimal.TEN, BigDecimal.TEN, null);
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
            Item i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });
        ItemResponse resp = itemService.criar(req);
        assertNotNull(resp);
        assertEquals(1L, resp.id());
        verify(eventPublisher).publishEstoqueBaixo(any(Item.class));
    }

    @Test
    void criarItemNaoPublicaEstoqueBaixoQuandoAcimaDoMinimo() {
        ItemRequest req = new ItemRequest("Resistor 10k", null, Categoria.INSUMO, "un", BigDecimal.TEN, BigDecimal.valueOf(5), null);
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
            Item i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });
        itemService.criar(req);
        verify(eventPublisher, never()).publishEstoqueBaixo(any());
    }

    @Test
    void listaComFiltroBaixoConsideraLimiteInclusivo() {
        Item ok = buildItem(1L, BigDecimal.valueOf(10), BigDecimal.valueOf(5));
        Item baixo = buildItem(2L, BigDecimal.valueOf(5), BigDecimal.valueOf(5));
        when(itemRepository.findAll()).thenReturn(List.of(ok, baixo));

        List<ItemResponse> resp = itemService.listar(null, null, true);

        assertEquals(1, resp.size());
        assertEquals(2L, resp.get(0).id());
    }

    @Test
    void baixarPorConsumoDiminuiEstoqueERegistraSaidas() {
        Item item1 = buildItem(1L, BigDecimal.valueOf(100), BigDecimal.ONE);
        Item item2 = buildItem(2L, BigDecimal.valueOf(50), BigDecimal.ONE);
        when(itemRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item1));
        when(itemRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(item2));
        when(saidaEstoqueRepository.existsByTipoSaidaAndIdReferencia(TipoSaida.CONSUMO, 100L)).thenReturn(false);
        when(saidaEstoqueRepository.save(any())).thenAnswer(inv -> {
            com.fablab.estoque.entity.SaidaEstoque s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        List<ItemConsumido> consumidos = List.of(
                new ItemConsumido(1L, BigDecimal.TEN),
                new ItemConsumido(2L, BigDecimal.valueOf(5)));
        List<SaidaResponse> saidas = itemService.baixarPorConsumo(consumidos, 100L);

        assertEquals(2, saidas.size());
        assertEquals(0, new BigDecimal("90.00").compareTo(item1.getQuantidadeAtual()));
        assertEquals(0, new BigDecimal("45.00").compareTo(item2.getQuantidadeAtual()));
    }

    @Test
    void baixarPorConsumoEIdempotentePorReferencia() {
        when(saidaEstoqueRepository.existsByTipoSaidaAndIdReferencia(TipoSaida.CONSUMO, 100L)).thenReturn(true);
        List<SaidaResponse> saidas = itemService.baixarPorConsumo(
                List.of(new ItemConsumido(1L, BigDecimal.TEN)), 100L);
        assertTrue(saidas.isEmpty());
    }

    @Test
    void baixarPorConsumoComSaldoInsuficienteLanca409() {
        Item item = buildItem(1L, BigDecimal.ONE, BigDecimal.ONE);
        when(itemRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item));
        when(saidaEstoqueRepository.existsByTipoSaidaAndIdReferencia(TipoSaida.CONSUMO, 100L)).thenReturn(false);

        assertThrows(SaldoInsuficienteException.class,
                () -> itemService.baixarPorConsumo(List.of(new ItemConsumido(1L, BigDecimal.TEN)), 100L));
    }

    @Test
    void exportarCsvGeraCabecalhoEItens() {
        Item item = buildItem(1L, BigDecimal.TEN, BigDecimal.ONE);
        Localizacao loc = new Localizacao();
        loc.setArmario("A1");
        loc.setPrateleira("P2");
        loc.setCaixa("C3");
        item.setLocalizacao(loc);
        when(itemRepository.findAll()).thenReturn(List.of(item));

        String csv = itemService.exportarCsv();

        assertTrue(csv.startsWith("id_item;nome;descricao;categoria;unidade_medida;quantidade_atual;estoque_minimo;localizacao"));
        assertTrue(csv.contains("A1/P2/C3"));
    }

    @Test
    void importarCsvCriaItensEmLote() {
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
            Item i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });
        String csv = "Placa MDF;Chapa 3mm;INSUMO;m2;10;2;\nParafuso;M3 x 10;PECA;un;200;50;";
        List<ItemResponse> importados = itemService.importarCsv(csv);
        assertEquals(2, importados.size());
    }

    @Test
    void buscarItemNaoEncontradoLancaExcecao() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> itemService.buscar(99L));
    }

    @Test
    void criarItemComLocalizacaoInexistenteLancaExcecao() {
        when(localizacaoRepository.findById(7L)).thenReturn(Optional.empty());
        ItemRequest req = new ItemRequest("X", null, Categoria.INSUMO, "un", BigDecimal.ONE, BigDecimal.ONE, 7L);
        assertThrows(ResourceNotFoundException.class, () -> itemService.criar(req));
    }
}