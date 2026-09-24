package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.CusteioDtos.CustoResponse;
import com.fablab.financeiro.dto.FinanceiroEventos.CustoCalculadoEvent;
import com.fablab.financeiro.dto.FinanceiroEventos.ProducaoConcluidaEvent;
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
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custeio por ordem (Job Order Costing): materiais (Σ SAIDA não canceladas por
 * referência) + mão de obra (Σ horas × valor/hora vigente, default nível 2
 * Voluntário) + overhead (taxa vigente × total de horas).
 */
@Service
public class CusteioService {

    /** Nível default para horas sem nível informado (premissa de MVP). */
    public static final int NIVEL_DEFAULT = 2;

    private final CustoEncomendaRepository custoRepository;
    private final FechamentoEncomendaRepository fechamentoRepository;
    private final HorasEncomendaRepository horasRepository;
    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final ParametroService parametroService;
    private final FinanceiroEventPublisher publisher;

    public CusteioService(CustoEncomendaRepository custoRepository,
                          FechamentoEncomendaRepository fechamentoRepository,
                          HorasEncomendaRepository horasRepository,
                          LancamentoFinanceiroRepository lancamentoRepository,
                          ParametroService parametroService,
                          FinanceiroEventPublisher publisher) {
        this.custoRepository = custoRepository;
        this.fechamentoRepository = fechamentoRepository;
        this.horasRepository = horasRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.parametroService = parametroService;
        this.publisher = publisher;
    }

    /** Calcula e congela o custo da encomenda (gatilho: {@code producao.concluida.event}). */
    @Transactional
    public CustoResponse calcular(Integer idEncomenda) {
        FechamentoEncomenda fechamento = fechamentoRepository.findByIdEncomenda(idEncomenda)
                .orElseThrow(() -> new IllegalStateException(
                        "Fechamento da encomenda " + idEncomenda + " não encontrado. Feche valor e horas antes do custeio."));

        BigDecimal custoMateriais = lancamentoRepository.somarPorTipoEReferencia(
                TipoLancamento.SAIDA, String.valueOf(idEncomenda));
        if (custoMateriais == null) {
            custoMateriais = BigDecimal.ZERO;
        }

        List<HorasEncomenda> linhas = horasRepository.findByIdEncomenda(idEncomenda);
        BigDecimal custoMaoObra = BigDecimal.ZERO;
        BigDecimal totalHoras = BigDecimal.ZERO;
        for (HorasEncomenda linha : linhas) {
            int nivel = linha.getNivelAcesso() != null ? linha.getNivelAcesso() : NIVEL_DEFAULT;
            BigDecimal valorHora = parametroService.obterValorHoraVigente(nivel);
            custoMaoObra = custoMaoObra.add(linha.getHoras().multiply(valorHora));
            totalHoras = totalHoras.add(linha.getHoras());
        }

        BigDecimal taxa = parametroService.obterTaxaVigente();
        BigDecimal custoOverhead = totalHoras.multiply(taxa);
        BigDecimal custoTotal = custoMateriais.add(custoMaoObra).add(custoOverhead);
        BigDecimal margem = fechamento.getValorFechado().subtract(custoTotal);

        CustoEncomenda entity = new CustoEncomenda();
        entity.setIdEncomenda(idEncomenda);
        entity.setCustoMateriais(custoMateriais);
        entity.setCustoMaoObra(custoMaoObra);
        entity.setCustoOverhead(custoOverhead);
        entity.setCustoTotal(custoTotal);
        entity.setValorVenda(fechamento.getValorFechado());
        entity.setMargemLucro(margem);
        entity.setDataCalculo(LocalDate.now());
        CustoEncomenda salvo = custoRepository.save(entity);

        publisher.publishCustoCalculado(new CustoCalculadoEvent(
                idEncomenda, custoTotal, margem, salvo.getDataCalculo()));
        return CustoResponse.of(salvo);
    }

    /** Variante síncrona para o listener de produção concluída. */
    @Transactional
    public CustoResponse calcularDoEvento(ProducaoConcluidaEvent event) {
        return calcular(event.idEncomenda());
    }

    /** Último cálculo da encomenda (congelado). */
    @Transactional(readOnly = true)
    public CustoResponse consultar(Integer idEncomenda) {
        return custoRepository.findFirstByIdEncomendaOrderByDataCalculoDescIdDesc(idEncomenda)
                .map(CustoResponse::of)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Custo da encomenda " + idEncomenda + " não encontrado"));
    }
}
