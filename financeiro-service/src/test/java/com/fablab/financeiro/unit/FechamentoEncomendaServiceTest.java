package com.fablab.financeiro.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.financeiro.dto.EncomendaCriadaEvent;
import com.fablab.financeiro.dto.FechamentoEncomendaRequest;
import com.fablab.financeiro.entity.FechamentoEncomenda;
import com.fablab.financeiro.entity.HorasEncomenda;
import com.fablab.financeiro.entity.StatusFechamentoEncomenda;
import com.fablab.financeiro.exception.ResourceNotFoundException;
import com.fablab.financeiro.repository.FechamentoEncomendaRepository;
import com.fablab.financeiro.repository.HorasEncomendaRepository;
import com.fablab.financeiro.service.FechamentoEncomendaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FechamentoEncomendaServiceTest {

    @Mock
    private FechamentoEncomendaRepository fechamentoRepository;
    @Mock
    private HorasEncomendaRepository horasRepository;

    @InjectMocks
    private FechamentoEncomendaService service;

    private FechamentoEncomenda fechamento(Long idEncomenda) {
        FechamentoEncomenda f = new FechamentoEncomenda();
        f.setIdFechamento(1L);
        f.setIdEncomenda(idEncomenda);
        f.setValorFechado(BigDecimal.valueOf(100));
        f.setDataFechamento(LocalDate.now());
        f.setStatus(StatusFechamentoEncomenda.ABERTA);
        f.setHorasValidadas(BigDecimal.ZERO);
        return f;
    }

    @Test
    void criarAbreFechamento() {
        when(fechamentoRepository.existsByIdEncomenda(1L)).thenReturn(false);
        when(fechamentoRepository.save(any(FechamentoEncomenda.class))).thenAnswer(inv -> {
            FechamentoEncomenda f = inv.getArgument(0);
            f.setIdFechamento(1L);
            return f;
        });

        var response = service.criar(new FechamentoEncomendaRequest(
                1L, BigDecimal.valueOf(10), BigDecimal.valueOf(500), LocalDate.now()));

        assertEquals(StatusFechamentoEncomenda.ABERTA, response.status());
        assertEquals(BigDecimal.ZERO, response.horasValidadas());
    }

    @Test
    void criarRejeitaFechamentoDuplicado() {
        when(fechamentoRepository.existsByIdEncomenda(1L)).thenReturn(true);
        var request = new FechamentoEncomendaRequest(1L, BigDecimal.ONE, BigDecimal.TEN, LocalDate.now());
        assertThrows(IllegalArgumentException.class, () -> service.criar(request));
        verify(fechamentoRepository, never()).save(any());
    }

    @Test
    void criarDoEventoUsaValorFinalEData() {
        when(fechamentoRepository.existsByIdEncomenda(7L)).thenReturn(false);
        when(fechamentoRepository.save(any(FechamentoEncomenda.class))).thenAnswer(inv -> inv.getArgument(0));

        var event = new EncomendaCriadaEvent(7L, 3L, "FILA", BigDecimal.valueOf(250), LocalDate.now());
        FechamentoEncomenda criado = service.criarDoEvento(event);

        assertEquals(BigDecimal.valueOf(250), criado.getValorFechado());
        assertEquals(StatusFechamentoEncomenda.ABERTA, criado.getStatus());
    }

    @Test
    void criarDoEventoEhIdempotente() {
        when(fechamentoRepository.existsByIdEncomenda(7L)).thenReturn(true);
        when(fechamentoRepository.findByIdEncomenda(7L)).thenReturn(Optional.of(fechamento(7L)));

        var event = new EncomendaCriadaEvent(7L, 3L, "FILA", BigDecimal.TEN, LocalDate.now());
        FechamentoEncomenda resultado = service.criarDoEvento(event);

        assertEquals(1L, resultado.getIdFechamento());
        verify(fechamentoRepository, never()).save(any());
    }

    @Test
    void obterPorEncomendaInexistenteLancaErro() {
        when(fechamentoRepository.findByIdEncomenda(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.obterPorEncomenda(1L));
    }

    @Test
    void registrarHorasCriaLinhaEAtualizaFechamento() {
        when(horasRepository.findFirstByIdEncomendaAndIdFuncionarioAndDataRegistro(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(fechamentoRepository.findByIdEncomenda(1L)).thenReturn(Optional.of(fechamento(1L)));

        service.registrarHorasValidadas(1L, 5L, 2, BigDecimal.valueOf(8), LocalDate.now());

        verify(horasRepository).save(any(HorasEncomenda.class));
        verify(fechamentoRepository).save(any(FechamentoEncomenda.class));
    }

    @Test
    void registrarHorasAcumulaLinhaExistente() {
        HorasEncomenda existente = new HorasEncomenda();
        existente.setIdEncomenda(1L);
        existente.setIdFuncionario(5L);
        existente.setNivelAcesso(1);
        existente.setHoras(BigDecimal.valueOf(4));
        existente.setDataRegistro(LocalDate.now());
        when(horasRepository.findFirstByIdEncomendaAndIdFuncionarioAndDataRegistro(any(), any(), any()))
                .thenReturn(Optional.of(existente));
        when(fechamentoRepository.findByIdEncomenda(1L)).thenReturn(Optional.empty());

        service.registrarHorasValidadas(1L, 5L, 2, BigDecimal.valueOf(6), LocalDate.now());

        assertEquals(BigDecimal.valueOf(10), existente.getHoras());
        assertEquals(1, existente.getNivelAcesso());
    }

    @Test
    void criarDoEventoSemValorFinalUsaZero() {
        when(fechamentoRepository.existsByIdEncomenda(9L)).thenReturn(false);
        when(fechamentoRepository.save(any(FechamentoEncomenda.class))).thenAnswer(inv -> inv.getArgument(0));

        var event = new EncomendaCriadaEvent(9L, 3L, "FILA", null, null);
        FechamentoEncomenda criado = service.criarDoEvento(event);

        assertEquals(BigDecimal.ZERO, criado.getValorFechado());
        assertEquals(LocalDate.now(), criado.getDataFechamento());
    }
}