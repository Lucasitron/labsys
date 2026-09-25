package com.fablab.estoque.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.fablab.estoque.dto.EmprestimoResponse;
import com.fablab.estoque.dto.EstoquePrincipal;
import com.fablab.estoque.entity.Categoria;
import com.fablab.estoque.entity.Emprestimo;
import com.fablab.estoque.entity.Item;
import com.fablab.estoque.entity.NivelAcesso;
import com.fablab.estoque.entity.StatusEmprestimo;
import com.fablab.estoque.rabbit.EstoqueEventPublisher;
import com.fablab.estoque.repository.EmprestimoRepository;
import com.fablab.estoque.repository.ItemRepository;
import com.fablab.estoque.repository.SaidaEstoqueRepository;
import com.fablab.estoque.service.EmprestimoService;
import com.fablab.estoque.service.RhPessoaClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * E-4: nome do tomador resolvido via RH, com fallback fail-soft.
 */
@ExtendWith(MockitoExtension.class)
class EmprestimoTomadorNomeTest {

    @Mock
    private EmprestimoRepository emprestimoRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private SaidaEstoqueRepository saidaEstoqueRepository;
    @Mock
    private EstoqueEventPublisher eventPublisher;
    @Mock
    private RhPessoaClient rhPessoaClient;

    private EstoquePrincipal adminPrincipal() {
        return new EstoquePrincipal(1L, NivelAcesso.ADMIN, "FabLab");
    }

    private Item buildItem() {
        Item item = new Item();
        item.setId(1L);
        item.setNome("Furadeira");
        item.setCategoria(Categoria.FERRAMENTA);
        item.setUnidadeMedida("un");
        item.setQuantidadeAtual(BigDecimal.TEN);
        item.setEstoqueMinimo(BigDecimal.ONE);
        return item;
    }

    private Emprestimo buildEmprestimo(Long id, Long idPessoa) {
        Emprestimo emp = new Emprestimo();
        emp.setId(id);
        emp.setIdPessoa(idPessoa);
        emp.setStatus(StatusEmprestimo.ATIVO);
        emp.setItem(buildItem());
        emp.setQuantidade(BigDecimal.ONE);
        emp.setDataEmprestimo(LocalDate.now());
        emp.setDataDevolucaoPrevista(LocalDate.now().plusDays(7));
        return emp;
    }

    private EmprestimoService service() {
        return new EmprestimoService(emprestimoRepository, itemRepository,
                saidaEstoqueRepository, eventPublisher, rhPessoaClient);
    }

    @Test
    void detalheResolveNomeDoTomadorViaRh() {
        when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(buildEmprestimo(1L, 10L)));
        when(rhPessoaClient.buscarNome(10L)).thenReturn(Optional.of("Maria Silva"));

        EmprestimoResponse resp = service().buscar(1L, adminPrincipal());

        assertEquals("Maria Silva", resp.pessoa());
        assertEquals(10L, resp.idPessoa());
    }

    @Test
    void listaGlobalResolveNomeDoTomadorViaRh() {
        when(emprestimoRepository.findAll())
                .thenReturn(List.of(buildEmprestimo(1L, 10L), buildEmprestimo(2L, 20L)));
        when(rhPessoaClient.buscarNome(10L)).thenReturn(Optional.of("Maria Silva"));
        when(rhPessoaClient.buscarNome(20L)).thenReturn(Optional.of("João Souza"));

        List<EmprestimoResponse> resp = service().listar(null, adminPrincipal());

        assertEquals(2, resp.size());
        assertEquals("Maria Silva", resp.get(0).pessoa());
        assertEquals("João Souza", resp.get(1).pessoa());
    }

    @Test
    void rhIndisponivelUsaRotuloEstavel() {
        when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(buildEmprestimo(1L, 10L)));
        when(rhPessoaClient.buscarNome(10L)).thenReturn(Optional.empty());

        EmprestimoResponse detalhe = service().buscar(1L, adminPrincipal());

        assertEquals("Pessoa #10", detalhe.pessoa());

        when(emprestimoRepository.findAll()).thenReturn(List.of(buildEmprestimo(1L, 10L)));
        List<EmprestimoResponse> lista = service().listar(null, adminPrincipal());

        assertEquals(1, lista.size());
        assertEquals("Pessoa #10", lista.get(0).pessoa());
    }

    @Test
    void semClientRhUsaRotuloEstavel() {
        when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(buildEmprestimo(1L, 10L)));
        EmprestimoService semRh = new EmprestimoService(emprestimoRepository, itemRepository,
                saidaEstoqueRepository, eventPublisher);

        EmprestimoResponse resp = semRh.buscar(1L, adminPrincipal());

        assertEquals("Pessoa #10", resp.pessoa());
    }
}
