package com.fablab.vendas.integration;

import com.fablab.vendas.dto.ClienteDtos.BulkTagRequest;
import com.fablab.vendas.dto.ClienteDtos.ClienteRequest;
import com.fablab.vendas.dto.CrmDtos.InteracaoRequest;
import com.fablab.vendas.dto.CrmDtos.TarefaRequest;
import com.fablab.vendas.dto.EncomendaDtos.EncomendaDetalheResponse;
import com.fablab.vendas.dto.EncomendaDtos.EncomendaRequest;
import com.fablab.vendas.dto.EncomendaDtos.KanbanRequest;
import com.fablab.vendas.dto.EncomendaDtos.NovaOrdemRequest;
import com.fablab.vendas.dto.MarketplaceDtos.MarketplaceListaResponse;
import com.fablab.vendas.dto.MarketplaceDtos.MarketplaceRequest;
import com.fablab.vendas.dto.OrcamentoDtos.ItemOrcamentoDto;
import com.fablab.vendas.dto.OrcamentoDtos.OrcamentoAtualizacaoRequest;
import com.fablab.vendas.dto.OrcamentoDtos.OrcamentoDetalheResponse;
import com.fablab.vendas.dto.OrcamentoDtos.OrcamentoRequest;
import com.fablab.vendas.dto.SolicitacaoDtos.DecisaoRequest;
import com.fablab.vendas.dto.SolicitacaoDtos.SolicitacaoRequest;
import com.fablab.vendas.dto.TagDtos.TagRequest;
import com.fablab.vendas.dto.TagDtos.TagResponse;
import com.fablab.vendas.dto.VendasPrincipal;
import com.fablab.vendas.entity.NivelAcesso;
import com.fablab.vendas.exception.ConflitoException;
import com.fablab.vendas.rabbit.VendasEventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.fablab.vendas.service.ClienteService;
import com.fablab.vendas.service.CrmService;
import com.fablab.vendas.service.EncomendaService;
import com.fablab.vendas.service.MarketplaceService;
import com.fablab.vendas.service.OrcamentoService;
import com.fablab.vendas.service.SolicitacaoService;
import com.fablab.vendas.service.TagService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Fluxo ponta a ponta: cliente → orçamento → aprovação → encomenda → Kanban →
 * histórico → nova ordem → marketplace → CRM → tags → solicitações.
 */
@SpringBootTest
class VendasFlowTest {

    @Autowired
    private ClienteService clienteService;
    @Autowired
    private OrcamentoService orcamentoService;
    @Autowired
    private EncomendaService encomendaService;
    @Autowired
    private MarketplaceService marketplaceService;
    @Autowired
    private CrmService crmService;
    @Autowired
    private TagService tagService;
    @Autowired
    private SolicitacaoService solicitacaoService;

    @MockBean
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private VendasEventPublisher eventPublisher;

    private VendasPrincipal criador() {
        return new VendasPrincipal(10L, NivelAcesso.BOLSISTA, "vendas");
    }

    private VendasPrincipal admin() {
        return new VendasPrincipal(1L, NivelAcesso.ADMIN, "admin");
    }

    @Test
    void fluxoCompleto() {
        var cliente = clienteService.criar(new ClienteRequest("PF", "Maria Oliveira",
                "529.982.247-25", "maria@email.com", "(48) 99999-0042", "Rua A, 123"), criador());
        assertEquals("PF", cliente.tipoPessoa());
        assertEquals(false, cliente.documento().contains("52998224725"));

        var orcamento = orcamentoService.criar(new OrcamentoRequest(cliente.id(),
                LocalDate.now().plusDays(30), "Suportes",
                List.of(new ItemOrcamentoDto("Suporte PLA", new BigDecimal("10"),
                        new BigDecimal("12.00"), "PLA", new BigDecimal("0.150"), "kg",
                        new BigDecimal("0.50"), false))), criador());
        assertEquals(new BigDecimal("120.00"), orcamento.valorTotal());
        assertEquals("Pendente", orcamento.status());

        var ajustado = orcamentoService.atualizar(orcamento.id(),
                new OrcamentoAtualizacaoRequest(null, null, "Ajuste", List.of(
                        new ItemOrcamentoDto("Suporte PLA", new BigDecimal("10"),
                                new BigDecimal("13.00"), "PLA", new BigDecimal("0.150"), "kg",
                                new BigDecimal("0.50"), false))), criador());
        assertEquals("Ajuste", ajustado.status());
        assertEquals(new BigDecimal("130.00"), ajustado.valorTotal());

        var aprovado = orcamentoService.atualizar(orcamento.id(),
                new OrcamentoAtualizacaoRequest(null, null, "Aprovado", null), criador());
        assertEquals("Aprovado", aprovado.status());
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("fablab.vendas"), eq("orcamento.aprovado.event"), any(Object.class));

        var pendente = orcamentoService.criar(new OrcamentoRequest(cliente.id(),
                LocalDate.now().plusDays(30), "Aguardando",
                List.of(new ItemOrcamentoDto("Peça", new BigDecimal("1"),
                        new BigDecimal("50.00"), null, null, null, null, false))), criador());
        assertThrows(ConflitoException.class, () -> encomendaService.criar(
                new EncomendaRequest(pendente.id(), null, null, null, null), criador()));

        var encomenda = encomendaService.criar(
                new EncomendaRequest(orcamento.id(), null, null,
                        LocalDate.now().plusDays(7), null), criador());
        assertEquals("Fila", encomenda.statusKanban());
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("fablab.vendas"), eq("encomenda.criada.event"), any(Object.class));

        assertThrows(ConflitoException.class, () -> encomendaService.criar(
                new EncomendaRequest(orcamento.id(), null, null, null, null), criador()));

        var movida = encomendaService.moverKanban(encomenda.id(),
                new KanbanRequest("Produção", encomenda.versao(), "Matéria-prima liberada"), criador());
        assertEquals("Produção", movida.statusKanban());
        assertEquals(2, movida.historico().size());
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("fablab.vendas"), eq("encomenda.status.alterado.event"), any(Object.class));

        assertThrows(ConflitoException.class, () -> encomendaService.moverKanban(encomenda.id(),
                new KanbanRequest("Acabamento", encomenda.versao(), null), criador()));

        EncomendaDetalheResponse novaOrdem = encomendaService.novaOrdem(encomenda.id(),
                new NovaOrdemRequest(LocalDate.now().plusDays(10), new BigDecimal("150.00"),
                        "Ajuste de escopo"), criador());
        assertEquals("Fila", novaOrdem.statusKanban());
        assertEquals(encomenda.id(), novaOrdem.encomendaOrigemId());

        var registro = marketplaceService.registrar(new MarketplaceRequest(novaOrdem.id(),
                "Mercado Livre", "ML-000001", LocalDate.now(), new BigDecimal("13.30")));
        assertEquals(new BigDecimal("136.70"), registro.valorLiquido());

        MarketplaceListaResponse lista = marketplaceService.listar(null, null);
        assertEquals(new BigDecimal("150.00"), lista.totais().get("bruto"));
        assertEquals(new BigDecimal("13.30"), lista.totais().get("taxas"));
        assertEquals(new BigDecimal("136.70"), lista.totais().get("liquido"));

        var interacao = crmService.registrarInteracao(new InteracaoRequest(cliente.id(),
                "WhatsApp", "Retorno sobre orçamento", null), criador());
        assertEquals("WhatsApp", interacao.tipo());
        assertEquals(1, crmService.listarInteracoes(cliente.id()).size());

        var tarefa = crmService.criarTarefa(new TarefaRequest("Campanha", "Redes sociais",
                10L, LocalDate.now(), LocalDate.now().plusDays(5), "Pendente", "Alta"), criador());
        assertEquals("Pendente", tarefa.status());
        var tarefas = crmService.listarTarefas(null, null);
        assertEquals(1L, tarefas.counts().get("Pendente"));

        TagResponse tag = tagService.criar(new TagRequest("VIP", "warn"));
        clienteService.vincularTag(cliente.id(), tag.id(), criador());
        var bulk = clienteService.bulkTag(new BulkTagRequest(List.of(cliente.id()), tag.id()), admin());
        assertEquals(0, bulk.get("vinculados"));

        var solicitacao = solicitacaoService.solicitar(new SolicitacaoRequest("ALTERACAO_DADOS",
                "CLIENTE", cliente.id(), "telefone", "(48) 99999-0042", "(48) 98888-0000",
                "Número atualizado"), criador());
        assertEquals("Pendente", solicitacao.status());
        var decidida = solicitacaoService.decidir(solicitacao.id(),
                new DecisaoRequest("APROVAR", null), admin());
        assertEquals("Aprovada", decidida.status());

        var duplicado = orcamentoService.duplicar(orcamento.id(), criador());
        assertEquals("Pendente", duplicado.status());
        assertEquals(aprovado.valorTotal(), duplicado.valorTotal());
        assertEquals(1, duplicado.itens().size());
    }
}
