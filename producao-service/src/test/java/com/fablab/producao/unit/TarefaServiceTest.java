package com.fablab.producao.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.producao.dto.TarefaRequest;
import com.fablab.producao.dto.TarefaStatusRequest;
import com.fablab.producao.entity.PrioridadeTarefa;
import com.fablab.producao.entity.Projeto;
import com.fablab.producao.entity.Tarefa;
import com.fablab.producao.entity.TarefaStatus;
import com.fablab.producao.exception.ForbiddenException;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.TarefaRepository;
import com.fablab.producao.service.AcessoService;
import com.fablab.producao.service.ProjetoService;
import com.fablab.producao.service.TarefaService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    @Mock
    private TarefaRepository tarefaRepository;
    @Mock
    private ProjetoService projetoService;
    @Mock
    private AcessoService acessoService;

    @InjectMocks
    private TarefaService service;

    private Projeto projeto(Long id, Long idResponsavel) {
        Projeto p = new Projeto();
        p.setIdProjeto(id);
        p.setIdResponsavel(idResponsavel);
        return p;
    }

    private Tarefa tarefa(Long id, Projeto projeto) {
        Tarefa t = new Tarefa();
        t.setIdTarefa(id);
        t.setProjeto(projeto);
        t.setTitulo("Montar estrutura");
        t.setStatus(TarefaStatus.PENDENTE);
        t.setPrioridade(PrioridadeTarefa.MEDIA);
        return t;
    }

    private TarefaRequest request(Long idProjeto) {
        return new TarefaRequest(idProjeto, "Montar estrutura", "desc", 5L,
                LocalDate.now(), LocalDate.now().plusDays(3), PrioridadeTarefa.ALTA);
    }

    @Test
    void criarDefineStatusPendenteEPrioridade() {
        Projeto projeto = projeto(1L, 5L);
        when(projetoService.obter(1L)).thenReturn(projeto);
        when(acessoService.podeEditar(5L)).thenReturn(true);
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> {
            Tarefa t = inv.getArgument(0);
            t.setIdTarefa(10L);
            return t;
        });

        var response = service.criar(request(1L));

        assertEquals(10L, response.idTarefa());
        assertEquals(TarefaStatus.PENDENTE, response.status());
        assertEquals(PrioridadeTarefa.ALTA, response.prioridade());
    }

    @Test
    void criarSemPrioridadeUsaMedia() {
        when(projetoService.obter(1L)).thenReturn(projeto(1L, 5L));
        when(acessoService.podeEditar(5L)).thenReturn(true);
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));

        var semPrioridade = new TarefaRequest(1L, "T", null, 5L, null, null, null);

        assertEquals(PrioridadeTarefa.MEDIA, service.criar(semPrioridade).prioridade());
    }

    @Test
    void criarQuandoProjetoDeOutroResponsavelEhProibido() {
        when(projetoService.obter(1L)).thenReturn(projeto(1L, 5L));
        when(acessoService.podeEditar(5L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.criar(request(1L)));
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void listarComProjetoUsaFiltro() {
        when(tarefaRepository.findByProjeto_IdProjeto(1L)).thenReturn(List.of(tarefa(1L, projeto(1L, 5L))));
        assertEquals(1, service.listar(1L, null, null).size());
    }

    @Test
    void listarComResponsavelUsaFiltro() {
        when(tarefaRepository.findByIdResponsavel(5L)).thenReturn(List.of(tarefa(1L, projeto(1L, 5L))));
        assertEquals(1, service.listar(null, 5L, null).size());
    }

    @Test
    void listarComStatusUsaFiltro() {
        when(tarefaRepository.findByStatus(TarefaStatus.PENDENTE))
                .thenReturn(List.of(tarefa(1L, projeto(1L, 5L))));
        assertEquals(1, service.listar(null, null, TarefaStatus.PENDENTE).size());
    }

    @Test
    void listarSemFiltroRetornaTodas() {
        when(tarefaRepository.findAll()).thenReturn(List.of(tarefa(1L, projeto(1L, 5L))));
        assertEquals(1, service.listar(null, null, null).size());
    }

    @Test
    void atualizarTarefa() {
        Tarefa tarefa = tarefa(1L, projeto(1L, 5L));
        when(tarefaRepository.findById(1L)).thenReturn(Optional.of(tarefa));
        when(acessoService.podeEditar(5L)).thenReturn(true);
        when(projetoService.obter(1L)).thenReturn(projeto(1L, 5L));

        var response = service.atualizar(1L, request(1L));

        assertEquals("Montar estrutura", response.titulo());
    }

    @Test
    void alterarStatusParaConcluidaDefineDataConclusao() {
        Tarefa tarefa = tarefa(1L, projeto(1L, 5L));
        when(tarefaRepository.findById(1L)).thenReturn(Optional.of(tarefa));
        when(acessoService.podeEditar(5L)).thenReturn(true);

        var response = service.alterarStatus(1L, new TarefaStatusRequest(TarefaStatus.CONCLUIDA, null));

        assertNotNull(response.dataConclusao());
        assertEquals(TarefaStatus.CONCLUIDA, response.status());
    }

    @Test
    void buscarInexistenteLancaExcecao() {
        when(tarefaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.buscar(99L));
    }

    @Test
    void removerTarefa() {
        Tarefa tarefa = tarefa(1L, projeto(1L, 5L));
        when(tarefaRepository.findById(1L)).thenReturn(Optional.of(tarefa));
        when(acessoService.podeEditar(5L)).thenReturn(true);

        service.remover(1L);

        verify(tarefaRepository).delete(tarefa);
    }
}