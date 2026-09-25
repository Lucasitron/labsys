package com.fablab.vendas.unit;

import com.fablab.vendas.dto.EncomendaDtos.EncomendaRequest;
import com.fablab.vendas.dto.EncomendaDtos.KanbanRequest;
import com.fablab.vendas.dto.VendasPrincipal;
import com.fablab.vendas.entity.Cliente;
import com.fablab.vendas.entity.Encomenda;
import com.fablab.vendas.entity.ItemOrcamento;
import com.fablab.vendas.entity.NivelAcesso;
import com.fablab.vendas.entity.Orcamento;
import com.fablab.vendas.entity.StatusKanban;
import com.fablab.vendas.entity.StatusOrcamento;
import com.fablab.vendas.entity.TipoPessoa;
import com.fablab.vendas.exception.ConflitoException;
import com.fablab.vendas.exception.ForbiddenException;
import com.fablab.vendas.service.DocumentoUtil;
import com.fablab.vendas.service.EncomendaService;
import com.fablab.vendas.service.MarketplaceService;
import com.fablab.vendas.service.OrcamentoService;
import com.fablab.vendas.service.PermissaoUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Regras de negócio: total do orçamento, bloqueio sem aprovação, lock otimista
 * do Kanban (D-9), líquido do marketplace e criador+Admin (D-3).
 */
@ExtendWith(MockitoExtension.class)
class RegrasVendasTest {

    @Mock
    private com.fablab.vendas.repository.OrcamentoRepository orcamentoRepository;
    @Mock
    private com.fablab.vendas.repository.ItemOrcamentoRepository itemRepository;
    @Mock
    private com.fablab.vendas.repository.ClienteRepository clienteRepository;
    @Mock
    private com.fablab.vendas.repository.EncomendaRepository encomendaRepository;
    @Mock
    private com.fablab.vendas.repository.HistoricoStatusEncomendaRepository historicoRepository;
    @Mock
    private com.fablab.vendas.repository.RegistroMarketplaceRepository registroRepository;
    @Mock
    private com.fablab.vendas.rabbit.VendasEventPublisher eventPublisher;

    private final PermissaoUtil permissaoUtil = new PermissaoUtil();
    private final DocumentoUtil documentoUtil = new DocumentoUtil();

    private VendasPrincipal bolsista(Long id) {
        return new VendasPrincipal(id, NivelAcesso.BOLSISTA, "vendas");
    }

    private ItemOrcamento item(String descricao, String qtd, String unitario) {
        ItemOrcamento item = new ItemOrcamento();
        item.setDescricao(descricao);
        item.setQuantidade(new BigDecimal(qtd));
        item.setValorUnitario(new BigDecimal(unitario));
        return item;
    }

    @Test
    void totalCalculadoDosItens() {
        OrcamentoService service = new OrcamentoService(orcamentoRepository, itemRepository,
                clienteRepository, encomendaRepository, eventPublisher, permissaoUtil);
        BigDecimal total = service.calcularTotal(List.of(
                item("Suporte PLA", "10", "12.00"),
                item("Corte laser", "2", "50.00")));
        assertEquals(new BigDecimal("220.00"), total);
    }

    @Test
    void encomendaBloqueadaSemAprovacao() {
        EncomendaService service = new EncomendaService(encomendaRepository, orcamentoRepository,
                clienteRepository, itemRepository, historicoRepository, eventPublisher, permissaoUtil);
        Orcamento orcamento = new Orcamento();
        orcamento.setId(1L);
        orcamento.setStatus(StatusOrcamento.Pendente);
        when(orcamentoRepository.findById(1L)).thenReturn(Optional.of(orcamento));

        EncomendaRequest request = new EncomendaRequest(1L, null, null, null, null);
        assertThrows(ConflitoException.class, () -> service.criar(request, bolsista(10L)));
    }

    @Test
    void kanbanConflitoDeVersao() {
        EncomendaService service = new EncomendaService(encomendaRepository, orcamentoRepository,
                clienteRepository, itemRepository, historicoRepository, eventPublisher, permissaoUtil);
        Encomenda encomenda = new Encomenda();
        encomenda.setId(1L);
        encomenda.setVersao(3L);
        encomenda.setStatusKanban(StatusKanban.Fila);
        encomenda.setCriadoPor(10L);
        when(encomendaRepository.findById(1L)).thenReturn(Optional.of(encomenda));

        KanbanRequest request = new KanbanRequest("Produção", 2L, null);
        assertThrows(ConflitoException.class, () -> service.moverKanban(1L, request, bolsista(10L)));
    }

    @Test
    void kanbanNegadoParaNaoCriador() {
        EncomendaService service = new EncomendaService(encomendaRepository, orcamentoRepository,
                clienteRepository, itemRepository, historicoRepository, eventPublisher, permissaoUtil);
        Encomenda encomenda = new Encomenda();
        encomenda.setId(1L);
        encomenda.setVersao(1L);
        encomenda.setStatusKanban(StatusKanban.Fila);
        encomenda.setCriadoPor(10L);
        when(encomendaRepository.findById(1L)).thenReturn(Optional.of(encomenda));

        KanbanRequest request = new KanbanRequest("Produção", 1L, null);
        assertThrows(ForbiddenException.class, () -> service.moverKanban(1L, request, bolsista(99L)));
    }

    @Test
    void liquidoDescontaTaxa() {
        MarketplaceService service = new MarketplaceService(registroRepository,
                encomendaRepository, clienteRepository, itemRepository);
        assertEquals(new BigDecimal("176.70"),
                service.liquido(new BigDecimal("190.00"), new BigDecimal("13.30")));
    }

    @Test
    void clienteExigeDocumentoValido() {
        assertThrows(IllegalArgumentException.class, () -> {
            if (!documentoUtil.valido("123.456.789-00")) {
                throw new IllegalArgumentException("CPF ou CNPJ inválido");
            }
        });
    }

    @Test
    void encomendaVendaDiretaExigeValor() {
        EncomendaService service = new EncomendaService(encomendaRepository, orcamentoRepository,
                clienteRepository, itemRepository, historicoRepository, eventPublisher, permissaoUtil);
        Cliente cliente = new Cliente();
        cliente.setId(5L);
        cliente.setTipoPessoa(TipoPessoa.PF);
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente));

        EncomendaRequest request = new EncomendaRequest(null, 5L, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.criar(request, bolsista(10L)));
    }

    @Test
    void transicaoKanbanInvalida() {
        EncomendaService service = new EncomendaService(encomendaRepository, orcamentoRepository,
                clienteRepository, itemRepository, historicoRepository, eventPublisher, permissaoUtil);
        Encomenda encomenda = new Encomenda();
        encomenda.setId(1L);
        encomenda.setVersao(1L);
        encomenda.setStatusKanban(StatusKanban.Fila);
        encomenda.setCriadoPor(10L);
        when(encomendaRepository.findById(1L)).thenReturn(Optional.of(encomenda));

        KanbanRequest request = new KanbanRequest("Inexistente", 1L, null);
        assertThrows(IllegalArgumentException.class, () -> service.moverKanban(1L, request, bolsista(10L)));
    }
}
