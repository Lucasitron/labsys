package com.fablab.producao.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.producao.dto.ChecklistItemRequest;
import com.fablab.producao.dto.MaterialSetorRequest;
import com.fablab.producao.dto.ResponsavelSetorRequest;
import com.fablab.producao.dto.SetorRequest;
import com.fablab.producao.dto.SinalizacaoSetorRequest;
import com.fablab.producao.entity.Setor;
import com.fablab.producao.entity.SetorChecklist;
import com.fablab.producao.entity.SetorMaterial;
import com.fablab.producao.entity.SetorResponsavel;
import com.fablab.producao.entity.SetorSinalizacao;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.SetorChecklistRepository;
import com.fablab.producao.repository.SetorMaterialRepository;
import com.fablab.producao.repository.SetorRepository;
import com.fablab.producao.repository.SetorResponsavelRepository;
import com.fablab.producao.repository.SetorSinalizacaoRepository;
import com.fablab.producao.service.SetorService;
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
class SetorServiceTest {

    @Mock
    private SetorRepository setorRepository;
    @Mock
    private SetorMaterialRepository materialRepository;
    @Mock
    private SetorSinalizacaoRepository sinalizacaoRepository;
    @Mock
    private SetorChecklistRepository checklistRepository;
    @Mock
    private SetorResponsavelRepository responsavelRepository;

    @InjectMocks
    private SetorService service;

    private Setor setor(Long id) {
        Setor s = new Setor();
        s.setIdSetor(id);
        s.setNumero(1);
        s.setNome("Marcenaria");
        s.setAtivo(true);
        return s;
    }

    @Test
    void criarSetor() {
        when(setorRepository.save(any(Setor.class))).thenAnswer(inv -> {
            Setor s = inv.getArgument(0);
            s.setIdSetor(1L);
            return s;
        });

        var response = service.criar(new SetorRequest(1, "Marcenaria", "d", "o", null, null, null));

        assertEquals(1L, response.idSetor());
        assertEquals("Marcenaria", response.nome());
    }

    @Test
    void listarAtivosUsaFiltro() {
        when(setorRepository.findByAtivoTrue()).thenReturn(List.of(setor(1L)));
        assertEquals(1, service.listar(true).size());
    }

    @Test
    void listarTodos() {
        when(setorRepository.findAll()).thenReturn(List.of(setor(1L)));
        assertEquals(1, service.listar(null).size());
    }

    @Test
    void buscarInexistenteLancaExcecao() {
        when(setorRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.buscar(99L));
    }

    @Test
    void atualizarSetor() {
        when(setorRepository.findById(1L)).thenReturn(Optional.of(setor(1L)));
        var response = service.atualizar(1L, new SetorRequest(2, "Corte", null, null, null, null, false));
        assertEquals("Corte", response.nome());
        assertFalse(response.ativo());
    }

    @Test
    void adicionarMaterial() {
        when(setorRepository.findById(1L)).thenReturn(Optional.of(setor(1L)));
        when(materialRepository.save(any(SetorMaterial.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.adicionarMaterial(1L, new MaterialSetorRequest("Parafuso", BigDecimal.TEN));

        assertEquals("Parafuso", response.descricao());
    }

    @Test
    void removerMaterialDeOutroSetorFalha() {
        SetorMaterial material = new SetorMaterial();
        material.setIdMaterial(9L);
        material.setSetor(setor(2L));
        when(materialRepository.findById(9L)).thenReturn(Optional.of(material));

        assertThrows(IllegalArgumentException.class, () -> service.removerMaterial(1L, 9L));
    }

    @Test
    void adicionarSinalizacao() {
        when(setorRepository.findById(1L)).thenReturn(Optional.of(setor(1L)));
        when(sinalizacaoRepository.save(any(SetorSinalizacao.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.adicionarSinalizacao(1L, new SinalizacaoSetorRequest("Saída"));

        assertEquals("Saída", response.texto());
    }

    @Test
    void adicionarChecklist() {
        when(setorRepository.findById(1L)).thenReturn(Optional.of(setor(1L)));
        when(checklistRepository.save(any(SetorChecklist.class))).thenAnswer(inv -> {
            SetorChecklist c = inv.getArgument(0);
            c.setIdChecklist(3L);
            return c;
        });

        var response = service.adicionarChecklist(1L, new ChecklistItemRequest("Bancada limpa", null));

        assertEquals(3L, response.idChecklist());
    }

    @Test
    void atualizarChecklist() {
        SetorChecklist checklist = new SetorChecklist();
        checklist.setIdChecklist(3L);
        checklist.setSetor(setor(1L));
        checklist.setItem("Antigo");
        checklist.setAtivo(true);
        when(checklistRepository.findById(3L)).thenReturn(Optional.of(checklist));

        var response = service.atualizarChecklist(1L, 3L, new ChecklistItemRequest("Novo", false));

        assertEquals("Novo", response.item());
        assertFalse(response.ativo());
    }

    @Test
    void adicionarResponsavelDesativaAnterior() {
        SetorResponsavel anterior = new SetorResponsavel();
        anterior.setAtivo(true);
        when(setorRepository.findById(1L)).thenReturn(Optional.of(setor(1L)));
        when(responsavelRepository.findFirstBySetor_IdSetorAndAtivoTrue(1L)).thenReturn(Optional.of(anterior));
        when(responsavelRepository.save(any(SetorResponsavel.class))).thenAnswer(inv -> {
            SetorResponsavel r = inv.getArgument(0);
            r.setIdResponsavel(4L);
            return r;
        });

        var response = service.adicionarResponsavel(1L,
                new ResponsavelSetorRequest(5L, LocalDate.now(), null, true));

        assertFalse(anterior.getAtivo());
        assertEquals(4L, response.idResponsavel());
    }

    @Test
    void removerResponsavel() {
        SetorResponsavel responsavel = new SetorResponsavel();
        responsavel.setIdResponsavel(4L);
        responsavel.setSetor(setor(1L));
        when(responsavelRepository.findById(4L)).thenReturn(Optional.of(responsavel));

        service.removerResponsavel(1L, 4L);

        verify(responsavelRepository).delete(responsavel);
    }

    @Test
    void atualizarAtivosDesativaResponsaveisVencidos() {
        SetorResponsavel vencido = new SetorResponsavel();
        vencido.setAtivo(true);
        vencido.setDataFim(LocalDate.now().minusDays(1));
        when(responsavelRepository.findBySetor_IdSetor(1L)).thenReturn(List.of(vencido));

        service.atualizarAtivos(1L);

        assertFalse(vencido.getAtivo());
    }
}