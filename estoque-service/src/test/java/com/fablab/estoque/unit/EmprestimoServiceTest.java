package com.fablab.estoque.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.estoque.dto.EmprestimoRequest;
import com.fablab.estoque.dto.EmprestimoResponse;
import com.fablab.estoque.dto.EstoquePrincipal;
import com.fablab.estoque.entity.Categoria;
import com.fablab.estoque.entity.Emprestimo;
import com.fablab.estoque.entity.Item;
import com.fablab.estoque.entity.NivelAcesso;
import com.fablab.estoque.entity.StatusEmprestimo;
import com.fablab.estoque.exception.ForbiddenException;
import com.fablab.estoque.exception.ResourceNotFoundException;
import com.fablab.estoque.exception.SaldoInsuficienteException;
import com.fablab.estoque.rabbit.EstoqueEventPublisher;
import com.fablab.estoque.repository.EmprestimoRepository;
import com.fablab.estoque.repository.ItemRepository;
import com.fablab.estoque.repository.SaidaEstoqueRepository;
import com.fablab.estoque.service.EmprestimoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmprestimoServiceTest {

    @Mock
    private EmprestimoRepository emprestimoRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private SaidaEstoqueRepository saidaEstoqueRepository;
    @Mock
    private EstoqueEventPublisher eventPublisher;
    @InjectMocks
    private EmprestimoService emprestimoService;

    private EstoquePrincipal adminPrincipal() {
        return new EstoquePrincipal(1L, NivelAcesso.ADMIN, "FabLab");
    }

    private EstoquePrincipal bolsistaPrincipal(Long idPessoa) {
        return new EstoquePrincipal(idPessoa, NivelAcesso.BOLSISTA, "FabLab");
    }

    private Item buildItem(Long id, BigDecimal qtd) {
        Item item = new Item();
        item.setId(id);
        item.setNome("Furadeira");
        item.setCategoria(Categoria.FERRAMENTA);
        item.setUnidadeMedida("un");
        item.setQuantidadeAtual(qtd);
        item.setEstoqueMinimo(BigDecimal.ONE);
        return item;
    }

    @Test
    void naoAdminNaoPodeEmprestarParaOutro() {
        EmprestimoRequest req = new EmprestimoRequest(1L, 99L, BigDecimal.ONE, LocalDate.now().plusDays(7), null);
        EmprestimoService service = new EmprestimoService(emprestimoRepository, itemRepository, saidaEstoqueRepository, eventPublisher);
        assertThrows(ForbiddenException.class,
                () -> service.criar(req, bolsistaPrincipal(1L)));
    }

    @Test
    void dataDevolucaoAnteriorAHojeLancaExcecao() {
        EmprestimoRequest req = new EmprestimoRequest(1L, 1L, BigDecimal.ONE, LocalDate.now().minusDays(1), null);
        EmprestimoService service = new EmprestimoService(emprestimoRepository, itemRepository, saidaEstoqueRepository, eventPublisher);
        assertThrows(IllegalArgumentException.class,
                () -> service.criar(req, adminPrincipal()));
    }

    @Test
    void estoqueInsuficienteLancaSaldoInsuficiente() {
        Item item = buildItem(1L, BigDecimal.ONE);
        when(itemRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item));
        EmprestimoRequest req = new EmprestimoRequest(1L, 1L, BigDecimal.TEN, LocalDate.now().plusDays(7), null);
        EmprestimoService service = new EmprestimoService(emprestimoRepository, itemRepository, saidaEstoqueRepository, eventPublisher);
        assertThrows(SaldoInsuficienteException.class,
                () -> service.criar(req, adminPrincipal()));
    }

    @Test
    void criarEmprestimoReduzEstoque() {
        Item item = buildItem(1L, BigDecimal.valueOf(10));
        when(itemRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item));
        when(emprestimoRepository.save(any())).thenAnswer(inv -> {
            Emprestimo e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EmprestimoRequest req = new EmprestimoRequest(1L, 1L, BigDecimal.valueOf(3), LocalDate.now().plusDays(7), null);
        EmprestimoService service = new EmprestimoService(emprestimoRepository, itemRepository, saidaEstoqueRepository, eventPublisher);
        EmprestimoResponse resp = service.criar(req, adminPrincipal());

        assertNotNull(resp);
        assertEquals(StatusEmprestimo.ATIVO, resp.status());
        assertEquals(0, new BigDecimal("7.00").compareTo(item.getQuantidadeAtual()));
    }

    @Test
    void devolverNaoAdminNaoResponsavelLancaExcecao() {
        Emprestimo emp = new Emprestimo();
        emp.setId(1L);
        emp.setIdPessoa(10L);
        emp.setStatus(StatusEmprestimo.ATIVO);
        when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(emp));

        EmprestimoService service = new EmprestimoService(emprestimoRepository, itemRepository, saidaEstoqueRepository, eventPublisher);
        assertThrows(ForbiddenException.class,
                () -> service.devolver(1L, bolsistaPrincipal(2L)));
    }

    @Test
    void devolverEmprestimoJaDevolvidoLancaExcecao() {
        Emprestimo emp = new Emprestimo();
        emp.setId(1L);
        emp.setIdPessoa(1L);
        emp.setStatus(StatusEmprestimo.DEVOLVIDO);
        when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(emp));

        EmprestimoService service = new EmprestimoService(emprestimoRepository, itemRepository, saidaEstoqueRepository, eventPublisher);
        assertThrows(IllegalArgumentException.class,
                () -> service.devolver(1L, adminPrincipal()));
    }

    @Test
    void devolverEmprestimoRestauraEstoque() {
        Emprestimo emp = new Emprestimo();
        emp.setId(1L);
        emp.setIdPessoa(10L);
        emp.setStatus(StatusEmprestimo.ATIVO);
        emp.setQuantidade(BigDecimal.valueOf(3));
        Item item = buildItem(1L, BigDecimal.valueOf(7));
        emp.setItem(item);
        when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(emp));
        when(itemRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item));

        EmprestimoService service = new EmprestimoService(emprestimoRepository, itemRepository, saidaEstoqueRepository, eventPublisher);
        EmprestimoResponse resp = service.devolver(1L, bolsistaPrincipal(10L));

        assertEquals(StatusEmprestimo.DEVOLVIDO, resp.status());
        assertEquals(0, new BigDecimal("10.00").compareTo(item.getQuantidadeAtual()));
    }

    @Test
    void verificarAtrasadosMarcaComoAtrasadoEPublica() {
        Emprestimo emp = new Emprestimo();
        emp.setId(1L);
        emp.setIdPessoa(10L);
        emp.setStatus(StatusEmprestimo.ATIVO);
        Item item = buildItem(1L, BigDecimal.valueOf(7));
        emp.setItem(item);
        emp.setDataDevolucaoPrevista(LocalDate.now().minusDays(1));
        when(emprestimoRepository.findByStatusInAndDataDevolucaoPrevistaBefore(
                any(), any())).thenReturn(List.of(emp));

        EmprestimoService service = new EmprestimoService(emprestimoRepository, itemRepository, saidaEstoqueRepository, eventPublisher);
        int marcados = service.verificarAtrasados();

        assertEquals(1, marcados);
        assertEquals(StatusEmprestimo.ATRASADO, emp.getStatus());
        verify(eventPublisher).publishEmprestimoAtrasado(emp);
    }

    @Test
    void buscarEmprestimoExistente() {
        Emprestimo emp = new Emprestimo();
        emp.setId(1L);
        emp.setIdPessoa(10L);
        emp.setStatus(StatusEmprestimo.ATIVO);
        Item item = buildItem(1L, BigDecimal.valueOf(7));
        emp.setItem(item);
        emp.setQuantidade(BigDecimal.ONE);
        emp.setDataEmprestimo(LocalDate.now());
        emp.setDataDevolucaoPrevista(LocalDate.now().plusDays(7));
        when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(emp));

        EmprestimoService service = new EmprestimoService(emprestimoRepository, itemRepository, saidaEstoqueRepository, eventPublisher);
        EmprestimoResponse resp = service.buscar(1L);

        assertEquals(1L, resp.id());
        assertEquals(StatusEmprestimo.ATIVO, resp.status());
    }
}