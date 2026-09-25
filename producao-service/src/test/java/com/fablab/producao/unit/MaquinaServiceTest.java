package com.fablab.producao.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.producao.dto.MaquinaRequest;
import com.fablab.producao.dto.MaquinaStatusRequest;
import com.fablab.producao.dto.UsoMaquinaFimRequest;
import com.fablab.producao.dto.UsoMaquinaRequest;
import com.fablab.producao.entity.HistoricoUsoMaquina;
import com.fablab.producao.entity.Maquina;
import com.fablab.producao.entity.MaquinaStatus;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.HistoricoUsoMaquinaRepository;
import com.fablab.producao.repository.MaquinaRepository;
import com.fablab.producao.service.MaquinaService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaquinaServiceTest {

    @Mock
    private MaquinaRepository maquinaRepository;
    @Mock
    private HistoricoUsoMaquinaRepository usoRepository;

    @InjectMocks
    private MaquinaService service;

    private Maquina maquina(Long id, MaquinaStatus status) {
        Maquina m = new Maquina();
        m.setIdMaquina(id);
        m.setNome("Impressora 3D");
        m.setStatus(status);
        return m;
    }

    @Test
    void criarDefineDisponivelQuandoSemStatus() {
        when(maquinaRepository.save(any(Maquina.class))).thenAnswer(inv -> {
            Maquina m = inv.getArgument(0);
            m.setIdMaquina(1L);
            return m;
        });

        var response = service.criar(new MaquinaRequest("Impressora 3D", "FDM", "Sala 2", null));

        assertEquals(MaquinaStatus.DISPONIVEL, response.status());
    }

    @Test
    void listarComStatusUsaFiltro() {
        when(maquinaRepository.findByStatus(MaquinaStatus.EM_USO))
                .thenReturn(List.of(maquina(1L, MaquinaStatus.EM_USO)));
        assertEquals(1, service.listar(MaquinaStatus.EM_USO).size());
    }

    @Test
    void listarSemStatusRetornaTodas() {
        when(maquinaRepository.findAll()).thenReturn(List.of(maquina(1L, MaquinaStatus.DISPONIVEL)));
        assertEquals(1, service.listar(null).size());
    }

    @Test
    void buscarInexistenteLancaExcecao() {
        when(maquinaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.buscar(99L));
    }

    @Test
    void atualizarMantemStatusExistente() {
        Maquina maquina = maquina(1L, MaquinaStatus.MANUTENCAO);
        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(maquina));

        var response = service.atualizar(1L, new MaquinaRequest("Impressora", "d", "Sala", null));

        assertEquals(MaquinaStatus.MANUTENCAO, response.status());
    }

    @Test
    void alterarStatus() {
        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(maquina(1L, MaquinaStatus.DISPONIVEL)));
        var response = service.alterarStatus(1L, new MaquinaStatusRequest(MaquinaStatus.MANUTENCAO));
        assertEquals(MaquinaStatus.MANUTENCAO, response.status());
    }

    @Test
    void iniciarUsoMarcaEmUso() {
        Maquina maquina = maquina(1L, MaquinaStatus.DISPONIVEL);
        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(maquina));
        when(usoRepository.save(any(HistoricoUsoMaquina.class))).thenAnswer(inv -> {
            HistoricoUsoMaquina u = inv.getArgument(0);
            u.setIdUso(9L);
            return u;
        });

        var response = service.iniciarUso(1L, new UsoMaquinaRequest(5L, null, "corte"));

        assertEquals(9L, response.idUso());
        assertEquals(MaquinaStatus.EM_USO, maquina.getStatus());
    }

    @Test
    void encerrarUsoCalculaHorasELiberaMaquina() {
        Maquina maquina = maquina(1L, MaquinaStatus.EM_USO);
        HistoricoUsoMaquina uso = new HistoricoUsoMaquina();
        uso.setIdUso(9L);
        uso.setMaquina(maquina);
        uso.setDataInicio(LocalDateTime.now().minusHours(2));
        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(maquina));
        when(usoRepository.findById(9L)).thenReturn(Optional.of(uso));

        var response = service.encerrarUso(1L, 9L, new UsoMaquinaFimRequest(LocalDateTime.now(), null));

        assertNotNull(response.horasUso());
        assertTrue(response.horasUso().intValue() >= 1);
        assertEquals(MaquinaStatus.DISPONIVEL, maquina.getStatus());
    }

    @Test
    void encerrarUsoDeMaquinaDiferenteFalha() {
        Maquina maquina = maquina(1L, MaquinaStatus.EM_USO);
        HistoricoUsoMaquina uso = new HistoricoUsoMaquina();
        uso.setIdUso(9L);
        uso.setMaquina(maquina(2L, MaquinaStatus.EM_USO));
        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(maquina));
        when(usoRepository.findById(9L)).thenReturn(Optional.of(uso));

        assertThrows(IllegalArgumentException.class,
                () -> service.encerrarUso(1L, 9L, new UsoMaquinaFimRequest(null, null)));
    }

    @Test
    void encerrarUsoComDataAnteriorFalha() {
        Maquina maquina = maquina(1L, MaquinaStatus.EM_USO);
        HistoricoUsoMaquina uso = new HistoricoUsoMaquina();
        uso.setIdUso(9L);
        uso.setMaquina(maquina);
        uso.setDataInicio(LocalDateTime.now());
        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(maquina));
        when(usoRepository.findById(9L)).thenReturn(Optional.of(uso));

        assertThrows(IllegalArgumentException.class,
                () -> service.encerrarUso(1L, 9L, new UsoMaquinaFimRequest(LocalDateTime.now().minusDays(1), null)));
    }

    @Test
    void historicoRetornaLista() {
        Maquina maquina = maquina(1L, MaquinaStatus.DISPONIVEL);
        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(maquina));
        when(usoRepository.findByMaquina_IdMaquinaOrderByDataInicioDesc(1L)).thenReturn(List.of());
        assertEquals(0, service.historico(1L).size());
    }

    @Test
    void removerMaquina() {
        Maquina maquina = maquina(1L, MaquinaStatus.DISPONIVEL);
        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(maquina));
        service.remover(1L);
        verify(maquinaRepository).delete(maquina);
    }
}