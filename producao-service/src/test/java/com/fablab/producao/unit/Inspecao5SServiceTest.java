package com.fablab.producao.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.producao.dto.Inspecao5SRequest;
import com.fablab.producao.dto.ItemInspecaoRequest;
import com.fablab.producao.entity.Inspecao5S;
import com.fablab.producao.entity.Setor;
import com.fablab.producao.entity.SetorChecklist;
import com.fablab.producao.entity.SetorResponsavel;
import com.fablab.producao.entity.StatusInspecao;
import com.fablab.producao.entity.TurnoInspecao;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.Inspecao5SRepository;
import com.fablab.producao.repository.SetorChecklistRepository;
import com.fablab.producao.repository.SetorResponsavelRepository;
import com.fablab.producao.service.AdvertenciaService;
import com.fablab.producao.service.Inspecao5SService;
import com.fablab.producao.service.SetorService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Inspecao5SServiceTest {

    @Mock
    private Inspecao5SRepository inspecaoRepository;
    @Mock
    private SetorChecklistRepository checklistRepository;
    @Mock
    private SetorResponsavelRepository responsavelRepository;
    @Mock
    private SetorService setorService;
    @Mock
    private AdvertenciaService advertenciaService;

    @InjectMocks
    private Inspecao5SService service;

    private Setor setor(Long id) {
        Setor s = new Setor();
        s.setIdSetor(id);
        s.setNome("Marcenaria");
        return s;
    }

    private SetorChecklist checklist(Long id, Setor setor) {
        SetorChecklist c = new SetorChecklist();
        c.setIdChecklist(id);
        c.setItem("Bancada limpa");
        c.setSetor(setor);
        c.setAtivo(true);
        return c;
    }

    private Inspecao5SRequest request(Long idSetor, boolean conforme) {
        return new Inspecao5SRequest(idSetor, 5L, LocalDate.now(), TurnoInspecao.MANHA, "obs",
                List.of(new ItemInspecaoRequest(1L, conforme, null)));
    }

    @Test
    void listarPorSetor() {
        when(inspecaoRepository.findBySetor_IdSetorOrderByDataInspecaoDesc(1L)).thenReturn(List.of());
        assertEquals(0, service.listar(1L).size());
    }

    @Test
    void listarTodas() {
        when(inspecaoRepository.findAll()).thenReturn(List.of());
        assertEquals(0, service.listar(null).size());
    }

    @Test
    void buscarInexistenteLancaExcecao() {
        when(inspecaoRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.buscar(9L));
    }

    @Test
    void registrarConformeResultaOk() {
        Setor setor = setor(1L);
        when(setorService.obter(1L)).thenReturn(setor);
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(checklist(1L, setor)));
        when(inspecaoRepository.save(any(Inspecao5S.class))).thenAnswer(inv -> {
            Inspecao5S i = inv.getArgument(0);
            i.setIdInspecao(7L);
            return i;
        });

        var response = service.registrar(request(1L, true));

        assertEquals(StatusInspecao.OK, response.status());
        verify(advertenciaService, never()).registrarAutomatica(anyLong(), any(), any());
    }

    @Test
    void registrarNaoConformeGeraAdvertenciaDoResponsavel() {
        Setor setor = setor(1L);
        when(setorService.obter(1L)).thenReturn(setor);
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(checklist(1L, setor)));
        when(inspecaoRepository.save(any(Inspecao5S.class))).thenAnswer(inv -> inv.getArgument(0));
        SetorResponsavel responsavel = new SetorResponsavel();
        responsavel.setIdFuncionario(5L);
        responsavel.setAtivo(true);
        when(responsavelRepository.findFirstBySetor_IdSetorAndAtivoTrue(1L)).thenReturn(Optional.of(responsavel));

        var response = service.registrar(request(1L, false));

        assertEquals(StatusInspecao.NAO_CONFORME, response.status());
        verify(advertenciaService).registrarAutomatica(eq(5L), any(Inspecao5S.class), any());
    }

    @Test
    void registrarComChecklistInexistenteFalha() {
        when(setorService.obter(1L)).thenReturn(setor(1L));
        when(checklistRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.registrar(request(1L, true)));
    }

    @Test
    void registrarComChecklistDeOutroSetorFalha() {
        when(setorService.obter(1L)).thenReturn(setor(1L));
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(checklist(1L, setor(2L))));

        assertThrows(IllegalArgumentException.class, () -> service.registrar(request(1L, true)));
    }

    @Test
    void removerInspecao() {
        Inspecao5S inspecao = new Inspecao5S();
        inspecao.setIdInspecao(7L);
        when(inspecaoRepository.findById(7L)).thenReturn(Optional.of(inspecao));

        service.remover(7L);

        verify(inspecaoRepository).delete(inspecao);
    }
}