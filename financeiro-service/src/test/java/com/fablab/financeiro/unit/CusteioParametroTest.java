package com.fablab.financeiro.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fablab.financeiro.dto.ParametroDtos.OverheadRequest;
import com.fablab.financeiro.dto.ParametroDtos.ValorHoraRequest;
import com.fablab.financeiro.entity.FechamentoEncomenda;
import com.fablab.financeiro.entity.HorasEncomenda;
import com.fablab.financeiro.entity.ParametroOverhead;
import com.fablab.financeiro.entity.StatusFechamento;
import com.fablab.financeiro.entity.TipoLancamento;
import com.fablab.financeiro.entity.ValorHoraNivel;
import com.fablab.financeiro.repository.CustoEncomendaRepository;
import com.fablab.financeiro.repository.FechamentoEncomendaRepository;
import com.fablab.financeiro.repository.HorasEncomendaRepository;
import com.fablab.financeiro.repository.LancamentoFinanceiroRepository;
import com.fablab.financeiro.repository.ParametroOverheadRepository;
import com.fablab.financeiro.repository.ValorHoraNivelRepository;
import com.fablab.financeiro.service.CusteioService;
import com.fablab.financeiro.service.FinanceiroEventPublisher;
import com.fablab.financeiro.service.ParametroService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Regras de custeio (Job Order Costing) e parâmetros: fórmulas, default de
 * nível 2, pré-condições e vigências.
 */
@ExtendWith(MockitoExtension.class)
class CusteioParametroTest {

    @Mock
    private CustoEncomendaRepository custoRepository;
    @Mock
    private FechamentoEncomendaRepository fechamentoRepository;
    @Mock
    private HorasEncomendaRepository horasRepository;
    @Mock
    private LancamentoFinanceiroRepository lancamentoRepository;
    @Mock
    private ValorHoraNivelRepository valorHoraRepository;
    @Mock
    private ParametroOverheadRepository overheadRepository;
    @Mock
    private FinanceiroEventPublisher publisher;

    private CusteioService custeioService;
    private ParametroService parametroService;

    @BeforeEach
    void setup() {
        parametroService = new ParametroService(valorHoraRepository, overheadRepository);
        custeioService = new CusteioService(custoRepository, fechamentoRepository,
                horasRepository, lancamentoRepository, parametroService, publisher);
    }

    private FechamentoEncomenda fechamento() {
        FechamentoEncomenda f = new FechamentoEncomenda();
        f.setId(1L);
        f.setIdEncomenda(2051);
        f.setHorasEstimadas(new BigDecimal("8.00"));
        f.setValorFechado(new BigDecimal("1240.00"));
        f.setDataFechamento(LocalDate.now());
        f.setStatus(StatusFechamento.ABERTA);
        f.setHorasValidadas(new BigDecimal("10.00"));
        return f;
    }

    private ValorHoraNivel valorHora(BigDecimal valor) {
        ValorHoraNivel v = new ValorHoraNivel();
        v.setId(1L);
        v.setNivelAcesso(1);
        v.setValorHora(valor);
        v.setDataVigencia(LocalDate.now());
        return v;
    }

    private ParametroOverhead overhead(BigDecimal taxa) {
        ParametroOverhead o = new ParametroOverhead();
        o.setId(1L);
        o.setValorTaxaHora(taxa);
        o.setDataVigencia(LocalDate.now());
        return o;
    }

    private HorasEncomenda horas(Integer nivel, String qtd) {
        HorasEncomenda h = new HorasEncomenda();
        h.setIdEncomenda(2051);
        h.setIdFuncionario(7);
        h.setNivelAcesso(nivel);
        h.setHoras(new BigDecimal(qtd));
        h.setDataRegistro(LocalDate.now());
        return h;
    }

    @Test
    void calculaMateriaisMaoDeObraOverheadMargem() {
        when(fechamentoRepository.findByIdEncomenda(2051)).thenReturn(Optional.of(fechamento()));
        when(lancamentoRepository.somarPorTipoEReferencia(TipoLancamento.SAIDA, "2051"))
                .thenReturn(new BigDecimal("348.20"));
        when(horasRepository.findByIdEncomenda(2051))
                .thenReturn(List.of(horas(1, "8.00"), horas(2, "2.00")));
        when(valorHoraRepository.findFirstByNivelAcessoOrderByDataVigenciaDesc(1))
                .thenReturn(Optional.of(valorHora(new BigDecimal("35.00"))));
        ValorHoraNivel voluntario = valorHora(new BigDecimal("10.00"));
        voluntario.setNivelAcesso(2);
        when(valorHoraRepository.findFirstByNivelAcessoOrderByDataVigenciaDesc(2))
                .thenReturn(Optional.of(voluntario));
        when(overheadRepository.findFirstByOrderByDataVigenciaDesc())
                .thenReturn(Optional.of(overhead(new BigDecimal("12.00"))));
        when(custoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var custo = custeioService.calcular(2051);

        // mão de obra: 8×35 + 2×10 = 300; overhead: 10×12 = 120; total: 348.20+300+120 = 768.20
        assertThat(custo.custoMateriais()).isEqualByComparingTo("348.20");
        assertThat(custo.custoMaoObra()).isEqualByComparingTo("300.00");
        assertThat(custo.custoOverhead()).isEqualByComparingTo("120.00");
        assertThat(custo.custoTotal()).isEqualByComparingTo("768.20");
        assertThat(custo.margemLucro()).isEqualByComparingTo("471.80");
    }

    @Test
    void horasSemNivelUsamNivel2Voluntario() {
        when(fechamentoRepository.findByIdEncomenda(2051)).thenReturn(Optional.of(fechamento()));
        when(lancamentoRepository.somarPorTipoEReferencia(TipoLancamento.SAIDA, "2051"))
                .thenReturn(BigDecimal.ZERO);
        when(horasRepository.findByIdEncomenda(2051)).thenReturn(List.of(horas(null, "4.00")));
        ValorHoraNivel voluntario = valorHora(new BigDecimal("10.00"));
        voluntario.setNivelAcesso(2);
        when(valorHoraRepository.findFirstByNivelAcessoOrderByDataVigenciaDesc(2))
                .thenReturn(Optional.of(voluntario));
        when(overheadRepository.findFirstByOrderByDataVigenciaDesc())
                .thenReturn(Optional.of(overhead(new BigDecimal("12.00"))));
        when(custoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var custo = custeioService.calcular(2051);

        assertThat(custo.custoMaoObra()).isEqualByComparingTo("40.00");
    }

    @Test
    void semFechamentoOuParametrosGeraErroOrientativo() {
        when(fechamentoRepository.findByIdEncomenda(9999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> custeioService.calcular(9999))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fechamento");

        when(fechamentoRepository.findByIdEncomenda(2051)).thenReturn(Optional.of(fechamento()));
        when(lancamentoRepository.somarPorTipoEReferencia(TipoLancamento.SAIDA, "2051"))
                .thenReturn(BigDecimal.ZERO);
        when(horasRepository.findByIdEncomenda(2051)).thenReturn(List.of(horas(1, "1.00")));
        when(valorHoraRepository.findFirstByNivelAcessoOrderByDataVigenciaDesc(1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> custeioService.calcular(2051))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Valor/hora do nível 1");
    }

    @Test
    void semOverheadGeraErroOrientativo() {
        when(fechamentoRepository.findByIdEncomenda(2051)).thenReturn(Optional.of(fechamento()));
        when(lancamentoRepository.somarPorTipoEReferencia(TipoLancamento.SAIDA, "2051"))
                .thenReturn(BigDecimal.ZERO);
        when(horasRepository.findByIdEncomenda(2051)).thenReturn(List.of());
        when(overheadRepository.findFirstByOrderByDataVigenciaDesc()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> custeioService.calcular(2051))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overhead");
    }

    @Test
    void nivelForaDe0a3RejeitadoNaValidacao() {
        var pedido = new ValorHoraRequest(5, new BigDecimal("10.00"), LocalDate.now());
        assertThat(pedido.nivelAcesso()).isGreaterThan(3);
    }

    @Test
    void vigenciaRetornaORegistroMaisRecente() {
        ValorHoraNivel antigo = valorHora(new BigDecimal("30.00"));
        antigo.setDataVigencia(LocalDate.now().minusDays(10));
        when(valorHoraRepository.findFirstByNivelAcessoOrderByDataVigenciaDesc(1))
                .thenReturn(Optional.of(valorHora(new BigDecimal("35.00"))));
        assertThat(parametroService.obterValorHoraVigente(1)).isEqualByComparingTo("35.00");

        when(overheadRepository.findFirstByOrderByDataVigenciaDesc())
                .thenReturn(Optional.of(overhead(new BigDecimal("12.00"))));
        assertThat(parametroService.obterTaxaVigente()).isEqualByComparingTo("12.00");
    }

    @Test
    void parametroAusenteGeraErroOrientativo() {
        when(valorHoraRepository.findFirstByNivelAcessoOrderByDataVigenciaDesc(0)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> parametroService.obterValorHoraVigente(0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("/valores-hora");
    }

    @Test
    void definirOverheadPersiste() {
        when(overheadRepository.save(any())).thenAnswer(i -> {
            com.fablab.financeiro.entity.ParametroOverhead o = i.getArgument(0);
            o.setId(3L);
            return o;
        });
        var resposta = parametroService.definirOverhead(
                new OverheadRequest(new BigDecimal("12.00"), LocalDate.now()), 1L);
        assertThat(resposta.id()).isEqualTo(3L);
    }
}
