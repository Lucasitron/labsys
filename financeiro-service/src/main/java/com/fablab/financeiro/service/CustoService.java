package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.CustoCalculadoEvent;
import com.fablab.financeiro.dto.CustoEncomendaResponse;
import com.fablab.financeiro.entity.CustoEncomenda;
import com.fablab.financeiro.entity.FechamentoEncomenda;
import com.fablab.financeiro.entity.HorasEncomenda;
import com.fablab.financeiro.entity.TipoLancamento;
import com.fablab.financeiro.exception.ResourceNotFoundException;
import com.fablab.financeiro.repository.CustoEncomendaRepository;
import com.fablab.financeiro.repository.FechamentoEncomendaRepository;
import com.fablab.financeiro.repository.HorasEncomendaRepository;
import com.fablab.financeiro.repository.LancamentoFinanceiroRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custeio por ordem (Job Order Costing): materiais + mão de obra + overhead.
 *
 * <p>Cálculos:
 * <ul>
 *   <li>materiais: soma dos lançamentos de saída com {@code idReferenciaExterna}
 *       igual ao id da encomenda (não cancelados);</li>
 *   <li>mão de obra: horas validadas × taxa vigente do nível; nível ausente
 *       tratado como 2 (VOLUNTARIO) — assunção de MVP, ver documentação;</li>
 *   <li>overhead: taxa de overhead vigente × total de horas validadas;</li>
 *   <li>margem: valor fechado − custo total.</li>
 * </ul></p>
 */
@Service
public class CustoService {

    private static final Integer NIVEL_PADRAO_SEM_INFORMACAO = 2;

    private final CustoEncomendaRepository custoRepository;
    private final FechamentoEncomendaRepository fechamentoRepository;
    private final HorasEncomendaRepository horasRepository;
    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final ValorHoraNivelService valorHoraService;
    private final ParametroOverheadService overheadService;
    private final FinanceiroEventPublisher eventPublisher;

    public CustoService(CustoEncomendaRepository custoRepository,
                        FechamentoEncomendaRepository fechamentoRepository,
                        HorasEncomendaRepository horasRepository,
                        LancamentoFinanceiroRepository lancamentoRepository,
                        ValorHoraNivelService valorHoraService,
                        ParametroOverheadService overheadService,
                        FinanceiroEventPublisher eventPublisher) {
        this.custoRepository = custoRepository;
        this.fechamentoRepository = fechamentoRepository;
        this.horasRepository = horasRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.valorHoraService = valorHoraService;
        this.overheadService = overheadService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CustoEncomendaResponse calcularCusto(Long idEncomenda) {
        FechamentoEncomenda fechamento = fechamentoRepository.findByIdEncomenda(idEncomenda)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Encomenda sem fechamento para cálculo de custo: " + idEncomenda));

        BigDecimal custoMateriais = lancamentoRepository.somaValoresPorTipoEReferencia(
                TipoLancamento.SAIDA, String.valueOf(idEncomenda));
        BigDecimal custoMaoObra = calcularCustoMaoObra(idEncomenda);
        BigDecimal totalHoras = horasRepository.somaHorasPorEncomenda(idEncomenda).orElse(BigDecimal.ZERO);
        BigDecimal taxaOverhead = overheadService.taxaVigente()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Configure o parâmetro de overhead antes de calcular o custo"))
                .getValorTaxaHora();
        BigDecimal custoOverhead = taxaOverhead.multiply(totalHoras);

        BigDecimal custoTotal = custoMateriais.add(custoMaoObra).add(custoOverhead);
        BigDecimal valorVenda = fechamento.getValorFechado();
        BigDecimal margemLucro = valorVenda.subtract(custoTotal);

        CustoEncomenda custo = new CustoEncomenda();
        custo.setIdEncomenda(idEncomenda);
        custo.setCustoMateriais(custoMateriais);
        custo.setCustoMaoObra(custoMaoObra);
        custo.setCustoOverhead(custoOverhead);
        custo.setCustoTotal(custoTotal);
        custo.setValorVenda(valorVenda);
        custo.setMargemLucro(margemLucro);
        custo.setDataCalculo(LocalDate.now());
        custo = custoRepository.save(custo);

        eventPublisher.publishCustoCalculado(
                new CustoCalculadoEvent(idEncomenda, custoTotal, margemLucro));
        return CustoEncomendaResponse.of(custo);
    }

    @Transactional(readOnly = true)
    public CustoEncomendaResponse obterPorEncomenda(Long idEncomenda) {
        return custoRepository.findFirstByIdEncomendaOrderByDataCalculoDesc(idEncomenda)
                .map(CustoEncomendaResponse::of)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Custo ainda não calculado para a encomenda: " + idEncomenda));
    }

    private BigDecimal calcularCustoMaoObra(Long idEncomenda) {
        Map<Integer, BigDecimal> horasPorNivel = new HashMap<>();
        for (HorasEncomenda linha : horasRepository.findAllByIdEncomenda(idEncomenda)) {
            Integer nivel = linha.getNivelAcesso() != null ? linha.getNivelAcesso() : NIVEL_PADRAO_SEM_INFORMACAO;
            horasPorNivel.merge(nivel, linha.getHoras(), BigDecimal::add);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, BigDecimal> entrada : horasPorNivel.entrySet()) {
            BigDecimal valorHora = valorHoraService.obterValorHoraPorNivel(entrada.getKey())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Defina o valor da hora para o nível " + entrada.getKey()))
                    .getValorHora();
            total = total.add(valorHora.multiply(entrada.getValue()));
        }
        return total;
    }
}