package com.fablab.producao.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.producao.dto.AdvertenciaLimiteEvent;
import com.fablab.producao.dto.AdvertenciaRegistradaEvent;
import com.fablab.producao.dto.AdvertenciaRequest;
import com.fablab.producao.entity.AdvertenciaMembro;
import com.fablab.producao.entity.Inspecao5S;
import com.fablab.producao.entity.TipoAdvertencia;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.AdvertenciaMembroRepository;
import com.fablab.producao.repository.Inspecao5SRepository;
import com.fablab.producao.service.AcessoService;
import com.fablab.producao.service.AdvertenciaService;
import com.fablab.producao.service.ProducaoEventPublisher;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdvertenciaServiceTest {

    @Mock
    private AdvertenciaMembroRepository advertenciaRepository;
    @Mock
    private Inspecao5SRepository inspecaoRepository;
    @Mock
    private ProducaoEventPublisher eventPublisher;
    @Mock
    private AcessoService acessoService;

    @InjectMocks
    private AdvertenciaService service;

    @Test
    void listarPorFuncionario() {
        when(advertenciaRepository.findByIdFuncionarioOrderByDataDesc(5L)).thenReturn(List.of());
        assertEquals(0, service.listar(5L).size());
    }

    @Test
    void listarTodas() {
        when(advertenciaRepository.findAll()).thenReturn(List.of());
        assertEquals(0, service.listar(null).size());
    }

    @Test
    void buscarInexistenteLancaExcecao() {
        when(advertenciaRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.buscar(9L));
    }

    @Test
    void registrarIncrementaContadorEPublicaEvento() {
        when(advertenciaRepository.countByIdFuncionario(5L)).thenReturn(0L);
        when(advertenciaRepository.save(any(AdvertenciaMembro.class))).thenAnswer(inv -> {
            AdvertenciaMembro a = inv.getArgument(0);
            a.setIdAdvertencia(1L);
            return a;
        });

        var response = service.registrar(new AdvertenciaRequest(5L, null, null, "Atraso", TipoAdvertencia.VERBAL));

        assertEquals(1, response.contador());
        verify(eventPublisher).publicarAdvertencia(any(AdvertenciaRegistradaEvent.class));
        verify(eventPublisher, never()).publicarAdvertenciaLimite(any());
    }

    @Test
    void registrarTerceiraAdvertenciaEmiteEventoCritico() {
        when(advertenciaRepository.countByIdFuncionario(5L)).thenReturn(2L);
        when(advertenciaRepository.save(any(AdvertenciaMembro.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.registrar(new AdvertenciaRequest(5L, null, LocalDate.now(), "Reincidência",
                TipoAdvertencia.FORMAL));

        assertEquals(3, response.contador());
        verify(eventPublisher).publicarAdvertenciaLimite(any(AdvertenciaLimiteEvent.class));
    }

    @Test
    void registrarComInspecaoExistente() {
        Inspecao5S inspecao = new Inspecao5S();
        inspecao.setIdInspecao(3L);
        when(inspecaoRepository.findById(3L)).thenReturn(Optional.of(inspecao));
        when(advertenciaRepository.countByIdFuncionario(5L)).thenReturn(0L);
        when(advertenciaRepository.save(any(AdvertenciaMembro.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.registrar(new AdvertenciaRequest(5L, 3L, null, "Não conformidade",
                TipoAdvertencia.VERBAL));

        assertEquals(3L, response.idInspecao());
    }

    @Test
    void registrarComInspecaoInexistenteFalha() {
        when(inspecaoRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.registrar(
                new AdvertenciaRequest(5L, 3L, null, "x", TipoAdvertencia.VERBAL)));
    }

    @Test
    void registrarAutomaticaUsaDadosDaInspecao() {
        when(advertenciaRepository.countByIdFuncionario(5L)).thenReturn(0L);
        when(advertenciaRepository.save(any(AdvertenciaMembro.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.registrarAutomatica(5L, null, "motivo");

        assertEquals(TipoAdvertencia.VERBAL, response.tipo());
        verify(eventPublisher).publicarAdvertencia(any(AdvertenciaRegistradaEvent.class));
    }
}