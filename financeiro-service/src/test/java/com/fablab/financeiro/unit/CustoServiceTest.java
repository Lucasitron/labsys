package com.fablab.financeiro.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.financeiro.dto.CustoCalculadoEvent;
import com.fablab.financeiro.entity.CustoEncomenda;
import com.fablab.financeiro.entity.FechamentoEncomenda;
import com.fablab.financeiro.entity.HorasEncomenda;
import com.fablab.financeiro.entity.ParametroOverhead;
import com.fablab.financeiro.entity.TipoLancamento;
import com.fablab.financeiro.entity.ValorHoraNivel;
import com.fablab.financeiro.exception.ResourceNotFoundException;
import com.fablab.financeiro.repository.CustoEncomendaRepository;
import com.fablab.financeiro.repository.FechamentoEncomendaRepository;
import com.fablab.financeiro.repository.HorasEncomendaRepository;
import com.fablab.financeiro.repository.LancamentoFinanceiroRepository;
import com.fablab.financeiro.service.CustoService;
import com.fablab.financeiro.service.FinanceiroEventPublisher;
import com.fablab.financeiro.service.ParametroOverheadService;
import com.fablab.financeiro.service.ValorHoraNivelService;
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
class CustoServiceTest {

    @Mock
    private CustoEncomendaRepository custoRepository;
    @Mock
    private FechamentoEncomendaRepository fechamentoRepository;
    @Mock
    private HorasEncomendaRepository horasRepository;
    @Mock
    private LancamentoFinanceiroRepository lancamentoRepository;
    @Mock
    private ValorHoraNivelService valorHoraService;
    @Mock
    private ParametroOverheadService overheadService;
    @Mock
    private FinanceiroEventPublisher eventPublisher;

    @InjectMocks
    private CustoService service;

    private FechamentoEncomenda fechamento(Long idEncomenda, BigDecimal valor) {
        FechamentoEncomenda f = new FechamentoEncomenda();
        f.setIdEncomenda(idEncomenda);
        f.setValorFechado(valor);
        return f;
    }

    private HorasEncomenda horas(Long idEncomenda, Long idFuncionario, Integer nivel, double horas) {
        HorasEncomenda h = new HorasEncomenda();
        h.setIdEncomenda(idEncomenda);
        h.setIdFuncionario(idFuncionario);
        h.setNivelAcesso(nivel);
        h.setHoras(BigDecimal.valueOf(horas));
        h.setDataRegistro(LocalDate.now());
        return h;
    }

    private ValorHoraNivel valorHora(int nivel, double valor) {
        ValorHoraNivel v = new ValorHoraNivel();
        v.setNivelAcesso(nivel);
        v.setValorHora(BigDecimal.valueOf(valor));
        return v;
    }

    @Test
    void calcularCustoAgregaMateriaisMaoDeObraEOverhead() {
        when(fechamentoRepository.findByIdEncomenda(1L))
                .thenReturn(Optional.of(fechamento(1L, BigDecimal.valueOf(1000))));
        when(lancamentoRepository.somaValoresPorTipoEReferencia(TipoLancamento.SAIDA, "1"))
                .thenReturn(BigDecimal.valueOf(200));
        when(horasRepository.findAllByIdEncomenda(1L)).thenReturn(List.of(
                horas(1L, 5L, 1, 2),
                horas(1L, 6L, null, 3)));
        when(valorHoraService.obterValorHoraPorNivel(1)).thenReturn(Optional.of(valorHora(1, 50)));
        when(valorHoraService.obterValorHoraPorNivel(2)).thenReturn(Optional.of(valorHora(2, 20)));
        when(horasRepository.somaHorasPorEncomenda(1L)).thenReturn(Optional.of(BigDecimal.valueOf(5)));
        ParametroOverhead overhead = new ParametroOverhead();
        overhead.setValorTaxaHora(BigDecimal.valueOf(5));
        when(overheadService.taxaVigente()).thenReturn(Optional.of(overhead));
        when(custoRepository.save(any(CustoEncomenda.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.calcularCusto(1L);

        assertEquals(BigDecimal.valueOf(200), response.custoMateriais());
        assertEquals(0, BigDecimal.valueOf(160).compareTo(response.custoMaoObra()));
        assertEquals(0, BigDecimal.valueOf(25).compareTo(response.custoOverhead()));
        assertEquals(0, BigDecimal.valueOf(385).compareTo(response.custoTotal()));
        assertEquals(0, BigDecimal.valueOf(615).compareTo(response.margemLucro()));
        verify(eventPublisher).publishCustoCalculado(any(CustoCalculadoEvent.class));
    }

    @Test
    void calcularCustoSemFechamentoLancaErro() {
        when(fechamentoRepository.findByIdEncomenda(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.calcularCusto(1L));
    }

    @Test
    void calcularCustoSemValorHoraLancaErro() {
        when(fechamentoRepository.findByIdEncomenda(1L))
                .thenReturn(Optional.of(fechamento(1L, BigDecimal.TEN)));
        when(lancamentoRepository.somaValoresPorTipoEReferencia(eq(TipoLancamento.SAIDA), any()))
                .thenReturn(BigDecimal.ZERO);
        when(horasRepository.findAllByIdEncomenda(1L)).thenReturn(List.of(horas(1L, 5L, 3, 2)));
        when(valorHoraService.obterValorHoraPorNivel(3)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.calcularCusto(1L));
    }

    @Test
    void calcularCustoSemOverheadLancaErro() {
        when(fechamentoRepository.findByIdEncomenda(1L))
                .thenReturn(Optional.of(fechamento(1L, BigDecimal.TEN)));
        when(lancamentoRepository.somaValoresPorTipoEReferencia(eq(TipoLancamento.SAIDA), any()))
                .thenReturn(BigDecimal.ZERO);
        when(horasRepository.findAllByIdEncomenda(1L)).thenReturn(List.of());
        when(horasRepository.somaHorasPorEncomenda(1L)).thenReturn(Optional.of(BigDecimal.ZERO));
        when(overheadService.taxaVigente()).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.calcularCusto(1L));
    }

    @Test
    void obterPorEncomendaRetornaCustoCalculado() {
        CustoEncomenda custo = new CustoEncomenda();
        custo.setIdEncomenda(1L);
        custo.setCustoTotal(BigDecimal.TEN);
        custo.setValorVenda(BigDecimal.valueOf(20));
        custo.setMargemLucro(BigDecimal.TEN);
        custo.setCustoMateriais(BigDecimal.ZERO);
        custo.setCustoMaoObra(BigDecimal.ZERO);
        custo.setCustoOverhead(BigDecimal.ZERO);
        custo.setDataCalculo(LocalDate.now());
        when(custoRepository.findFirstByIdEncomendaOrderByDataCalculoDesc(1L)).thenReturn(Optional.of(custo));

        var response = service.obterPorEncomenda(1L);

        assertEquals(BigDecimal.TEN, response.custoTotal());
    }

    @Test
    void obterPorEncomendaSemCalculoLancaErro() {
        when(custoRepository.findFirstByIdEncomendaOrderByDataCalculoDesc(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.obterPorEncomenda(1L));
    }
}