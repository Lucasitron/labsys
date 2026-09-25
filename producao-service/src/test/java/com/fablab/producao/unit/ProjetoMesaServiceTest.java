package com.fablab.producao.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.producao.dto.AuditoriaProjetoMesaRequest;
import com.fablab.producao.dto.ProjetoMesaAbandonadoEvent;
import com.fablab.producao.dto.ProjetoMesaRequest;
import com.fablab.producao.entity.AuditoriaProjetoMesa;
import com.fablab.producao.entity.ProjetoMesa;
import com.fablab.producao.entity.StatusProjetoMesa;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.AuditoriaProjetoMesaRepository;
import com.fablab.producao.repository.ProjetoMesaRepository;
import com.fablab.producao.service.AcessoService;
import com.fablab.producao.service.ProducaoEventPublisher;
import com.fablab.producao.service.ProjetoMesaService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjetoMesaServiceTest {

    @Mock
    private ProjetoMesaRepository projetoMesaRepository;
    @Mock
    private AuditoriaProjetoMesaRepository auditoriaRepository;
    @Mock
    private ProducaoEventPublisher eventPublisher;
    @Mock
    private AcessoService acessoService;

    @InjectMocks
    private ProjetoMesaService service;

    private ProjetoMesa projetoMesa(Long id, StatusProjetoMesa status) {
        ProjetoMesa p = new ProjetoMesa();
        p.setIdProjetoMesa(id);
        p.setIdFuncionario(5L);
        p.setIdMesa(1L);
        p.setNomeProjeto("Drone");
        p.setDataInicio(LocalDate.now());
        p.setDataUltimaEvolucao(LocalDate.now());
        p.setStatus(status);
        p.setQrCodeTotem("fablab://projeto-mesa/" + id);
        return p;
    }

    private ProjetoMesaRequest request() {
        return new ProjetoMesaRequest(5L, 1L, "Drone", "Aeroespacial", LocalDate.now().plusMonths(1));
    }

    @Test
    void criarGeraQrCode() {
        when(projetoMesaRepository.save(any(ProjetoMesa.class))).thenAnswer(inv -> {
            ProjetoMesa p = inv.getArgument(0);
            p.setIdProjetoMesa(1L);
            return p;
        });

        var response = service.criar(request());

        assertEquals(1L, response.idProjetoMesa());
        assertEquals("fablab://projeto-mesa/1", response.qrCodeTotem());
        assertEquals(StatusProjetoMesa.ATIVO, response.status());
    }

    @Test
    void listarPorStatus() {
        when(projetoMesaRepository.findByStatus(StatusProjetoMesa.ABANDONADO))
                .thenReturn(List.of(projetoMesa(1L, StatusProjetoMesa.ABANDONADO)));
        assertEquals(1, service.listar(StatusProjetoMesa.ABANDONADO).size());
    }

    @Test
    void listarTodos() {
        when(projetoMesaRepository.findAll()).thenReturn(List.of(projetoMesa(1L, StatusProjetoMesa.ATIVO)));
        assertEquals(1, service.listar(null).size());
    }

    @Test
    void buscarInexistenteLancaExcecao() {
        when(projetoMesaRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.buscar(9L));
    }

    @Test
    void atualizarProjeto() {
        when(projetoMesaRepository.findById(1L)).thenReturn(Optional.of(projetoMesa(1L, StatusProjetoMesa.ATIVO)));
        var response = service.atualizar(1L, new ProjetoMesaRequest(6L, 2L, "Robô", "Automação", null));
        assertEquals("Robô", response.nomeProjeto());
        assertEquals(6L, response.idFuncionario());
    }

    @Test
    void registrarEvolucaoReativaProjetoAbandonado() {
        ProjetoMesa projeto = projetoMesa(1L, StatusProjetoMesa.ABANDONADO);
        when(projetoMesaRepository.findById(1L)).thenReturn(Optional.of(projeto));

        var response = service.registrarEvolucao(1L);

        assertEquals(StatusProjetoMesa.ATIVO, response.status());
        assertNotNull(response.dataUltimaEvolucao());
    }

    @Test
    void conteudoQrCodeRetornaUrl() {
        when(projetoMesaRepository.findById(1L)).thenReturn(Optional.of(projetoMesa(1L, StatusProjetoMesa.ATIVO)));
        assertEquals("fablab://projeto-mesa/1", service.conteudoQrCode(1L));
    }

    @Test
    void auditarComoAbandonadoPublicaEvento() {
        ProjetoMesa projeto = projetoMesa(1L, StatusProjetoMesa.ATIVO);
        when(projetoMesaRepository.findById(1L)).thenReturn(Optional.of(projeto));
        when(auditoriaRepository.save(any(AuditoriaProjetoMesa.class))).thenAnswer(inv -> {
            AuditoriaProjetoMesa a = inv.getArgument(0);
            a.setIdAuditoria(3L);
            return a;
        });

        var response = service.auditar(new AuditoriaProjetoMesaRequest(1L, StatusProjetoMesa.ABANDONADO, "Recolher"));

        assertEquals(3L, response.idAuditoria());
        assertEquals(StatusProjetoMesa.ABANDONADO, projeto.getStatus());
        verify(eventPublisher).publicarProjetoMesaAbandonado(any(ProjetoMesaAbandonadoEvent.class));
    }

    @Test
    void auditarComoAtivoNaoPublicaEvento() {
        when(projetoMesaRepository.findById(1L)).thenReturn(Optional.of(projetoMesa(1L, StatusProjetoMesa.ATIVO)));
        when(auditoriaRepository.save(any(AuditoriaProjetoMesa.class))).thenAnswer(inv -> inv.getArgument(0));

        service.auditar(new AuditoriaProjetoMesaRequest(1L, StatusProjetoMesa.ATIVO, "Manter"));

        verify(eventPublisher, never()).publicarProjetoMesaAbandonado(any());
    }

    @Test
    void listarAuditorias() {
        when(projetoMesaRepository.findById(1L)).thenReturn(Optional.of(projetoMesa(1L, StatusProjetoMesa.ATIVO)));
        when(auditoriaRepository.findByProjetoMesa_IdProjetoMesa(1L)).thenReturn(List.of());
        assertEquals(0, service.listarAuditorias(1L).size());
    }

    @Test
    void projetosSemEvolucaoUsaFiltro() {
        when(projetoMesaRepository.findByStatusAndDataUltimaEvolucaoBefore(any(), any()))
                .thenReturn(List.of(projetoMesa(1L, StatusProjetoMesa.ATIVO)));
        assertEquals(1, service.projetosSemEvolucao(LocalDate.now().minusDays(15)).size());
    }

    @Test
    void removerProjeto() {
        ProjetoMesa projeto = projetoMesa(1L, StatusProjetoMesa.ATIVO);
        when(projetoMesaRepository.findById(1L)).thenReturn(Optional.of(projeto));
        service.remover(1L);
        verify(projetoMesaRepository).delete(projeto);
    }
}