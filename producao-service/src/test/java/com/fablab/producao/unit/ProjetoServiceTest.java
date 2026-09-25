package com.fablab.producao.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.producao.dto.ProjetoRequest;
import com.fablab.producao.dto.ProjetoStatusRequest;
import com.fablab.producao.entity.Projeto;
import com.fablab.producao.entity.ProjetoStatus;
import com.fablab.producao.exception.ForbiddenException;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.ProjetoRepository;
import com.fablab.producao.service.AcessoService;
import com.fablab.producao.service.ProjetoService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjetoServiceTest {

    @Mock
    private ProjetoRepository projetoRepository;
    @Mock
    private AcessoService acessoService;

    @InjectMocks
    private ProjetoService service;

    private ProjetoRequest request(Long idResponsavel) {
        return new ProjetoRequest("Braço Robótico", "Projeto de extensão",
                LocalDate.now(), LocalDate.now().plusMonths(2), idResponsavel, null);
    }

    private Projeto projeto(Long id, Long idResponsavel, ProjetoStatus status) {
        Projeto p = new Projeto();
        p.setIdProjeto(id);
        p.setNome("Braço Robótico");
        p.setDataInicio(LocalDate.now());
        p.setIdResponsavel(idResponsavel);
        p.setStatus(status);
        return p;
    }

    @Test
    void criarComoAdminPersiste() {
        when(acessoService.isAdmin()).thenReturn(true);
        when(projetoRepository.save(any(Projeto.class))).thenAnswer(inv -> {
            Projeto p = inv.getArgument(0);
            p.setIdProjeto(1L);
            return p;
        });

        var response = service.criar(request(5L));

        assertEquals(1L, response.idProjeto());
        assertEquals(ProjetoStatus.PLANEJADO, response.status());
    }

    @Test
    void criarComoResponsavelPersiste() {
        when(acessoService.isAdmin()).thenReturn(false);
        when(acessoService.podeEditar(5L)).thenReturn(true);
        when(projetoRepository.save(any(Projeto.class))).thenAnswer(inv -> inv.getArgument(0));

        assertNotNull(service.criar(request(5L)));
    }

    @Test
    void criarParaOutroResponsavelEhProibido() {
        when(acessoService.isAdmin()).thenReturn(false);
        when(acessoService.podeEditar(5L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.criar(request(5L)));
        verify(projetoRepository, never()).save(any());
    }

    @Test
    void listarComStatusUsaFiltroDeStatus() {
        when(projetoRepository.findByStatus(ProjetoStatus.EM_ANDAMENTO))
                .thenReturn(List.of(projeto(1L, 5L, ProjetoStatus.EM_ANDAMENTO)));

        assertEquals(1, service.listar(ProjetoStatus.EM_ANDAMENTO, null).size());
        verify(projetoRepository, never()).findAll();
    }

    @Test
    void listarComResponsavelUsaFiltroDeResponsavel() {
        when(projetoRepository.findByIdResponsavel(5L)).thenReturn(List.of(projeto(1L, 5L, ProjetoStatus.PLANEJADO)));

        assertEquals(1, service.listar(null, 5L).size());
    }

    @Test
    void listarSemFiltroRetornaTodos() {
        when(projetoRepository.findAll()).thenReturn(List.of(projeto(1L, 5L, ProjetoStatus.PLANEJADO)));

        assertEquals(1, service.listar(null, null).size());
    }

    @Test
    void buscarInexistenteLancaExcecao() {
        when(projetoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.buscar(99L));
    }

    @Test
    void atualizarProjetoDoResponsavel() {
        Projeto projeto = projeto(1L, 5L, ProjetoStatus.PLANEJADO);
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projeto));
        when(acessoService.podeEditar(5L)).thenReturn(true);

        var response = service.atualizar(1L, request(5L));

        assertEquals("Braço Robótico", response.nome());
    }

    @Test
    void atualizarProjetoDeOutroEhProibido() {
        Projeto projeto = projeto(1L, 5L, ProjetoStatus.PLANEJADO);
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projeto));
        when(acessoService.podeEditar(5L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.atualizar(1L, request(5L)));
    }

    @Test
    void alterarStatusParaConcluidoDefineDataFimReal() {
        Projeto projeto = projeto(1L, 5L, ProjetoStatus.EM_ANDAMENTO);
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projeto));
        when(acessoService.podeEditar(5L)).thenReturn(true);

        var response = service.alterarStatus(1L, new ProjetoStatusRequest(ProjetoStatus.CONCLUIDO, null));

        assertEquals(ProjetoStatus.CONCLUIDO, response.status());
        assertNotNull(response.dataFimReal());
    }

    @Test
    void alterarStatusParaOutroNaoDefineDataFimReal() {
        Projeto projeto = projeto(1L, 5L, ProjetoStatus.PLANEJADO);
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projeto));
        when(acessoService.podeEditar(5L)).thenReturn(true);

        var response = service.alterarStatus(1L, new ProjetoStatusRequest(ProjetoStatus.CANCELADO, null));

        assertEquals(ProjetoStatus.CANCELADO, response.status());
        assertEquals(null, response.dataFimReal());
    }

    @Test
    void removerProjetoDoResponsavel() {
        Projeto projeto = projeto(1L, 5L, ProjetoStatus.PLANEJADO);
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projeto));
        when(acessoService.podeEditar(5L)).thenReturn(true);

        service.remover(1L);

        verify(projetoRepository).delete(projeto);
    }
}