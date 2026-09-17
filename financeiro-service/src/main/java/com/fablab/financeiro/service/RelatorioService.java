package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.CustoMaquinaItemResponse;
import com.fablab.financeiro.dto.DoacoesDespesasResponse;
import com.fablab.financeiro.dto.DreResponse;
import com.fablab.financeiro.dto.FluxoCaixaResponse;
import com.fablab.financeiro.dto.InadimplenciaItemResponse;
import com.fablab.financeiro.dto.LucratividadeItemResponse;
import com.fablab.financeiro.entity.LancamentoFinanceiro;
import com.fablab.financeiro.entity.StatusLancamento;
import com.fablab.financeiro.entity.TipoDoacaoRecurso;
import com.fablab.financeiro.entity.TipoLancamento;
import com.fablab.financeiro.repository.CustoEncomendaRepository;
import com.fablab.financeiro.repository.DoacaoRecursoRepository;
import com.fablab.financeiro.repository.LancamentoFinanceiroRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relatórios financeiros: fluxo de caixa, DRE, lucratividade, inadimplência,
 * doações x despesas e custo por máquina.
 */
@Service
public class RelatorioService {

    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final DoacaoRecursoRepository doacaoRepository;
    private final CustoEncomendaRepository custoRepository;

    public RelatorioService(LancamentoFinanceiroRepository lancamentoRepository,
                            DoacaoRecursoRepository doacaoRepository,
                            CustoEncomendaRepository custoRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.doacaoRepository = doacaoRepository;
        this.custoRepository = custoRepository;
    }

    @Transactional(readOnly = true)
    public FluxoCaixaResponse fluxoCaixa(LocalDate inicio, LocalDate fim) {
        BigDecimal entradas = lancamentoRepository.somaValoresPorTipo(TipoLancamento.ENTRADA, inicio, fim);
        BigDecimal saidas = lancamentoRepository.somaValoresPorTipo(TipoLancamento.SAIDA, inicio, fim);
        return FluxoCaixaResponse.of(inicio, fim, entradas, saidas);
    }

    @Transactional(readOnly = true)
    public DreResponse dre(LocalDate inicio, LocalDate fim) {
        BigDecimal receitas = lancamentoRepository.somaValoresPorTipo(TipoLancamento.ENTRADA, inicio, fim)
                .add(doacaoRepository.somarValores(null, inicio, fim));
        BigDecimal despesas = lancamentoRepository.somaValoresPorTipo(TipoLancamento.SAIDA, inicio, fim);
        return DreResponse.of(receitas, despesas);
    }

    @Transactional(readOnly = true)
    public List<LucratividadeItemResponse> lucratividade() {
        return custoRepository.findAllByOrderByDataCalculoDesc().stream()
                .map(LucratividadeItemResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InadimplenciaItemResponse> inadimplencia() {
        return lancamentoRepository.findByStatusInAndDataVencimentoBefore(
                        List.of(StatusLancamento.PENDENTE, StatusLancamento.ATRASADO), LocalDate.now())
                .stream()
                .filter(l -> l.getTipo() == TipoLancamento.ENTRADA)
                .map(InadimplenciaItemResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public DoacoesDespesasResponse doacoesDespesas(LocalDate inicio, LocalDate fim) {
        BigDecimal doacoes = doacaoRepository.somarValores(TipoDoacaoRecurso.DOACAO, inicio, fim);
        BigDecimal despesas = lancamentoRepository.somaValoresPorTipo(TipoLancamento.SAIDA, inicio, fim);
        return DoacoesDespesasResponse.of(inicio, fim, doacoes, despesas);
    }

    /**
     * Custo por máquina: soma dos lançamentos de saída agrupados pela
     * {@code idReferenciaExterna}, que identifica a máquina — assunção de MVP,
     * ver documentação.
     */
    @Transactional(readOnly = true)
    public List<CustoMaquinaItemResponse> custoMaquina() {
        Map<String, BigDecimal> custoPorMaquina = new LinkedHashMap<>();
        for (LancamentoFinanceiro l : lancamentoRepository.findAll()) {
            if (l.getTipo() == TipoLancamento.SAIDA
                    && l.getStatus() != StatusLancamento.CANCELADO
                    && l.getIdReferenciaExterna() != null
                    && !l.getIdReferenciaExterna().isBlank()) {
                custoPorMaquina.merge(l.getIdReferenciaExterna(), l.getValor(), BigDecimal::add);
            }
        }
        return custoPorMaquina.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(e -> new CustoMaquinaItemResponse(e.getKey(), e.getValue()))
                .toList();
    }
}