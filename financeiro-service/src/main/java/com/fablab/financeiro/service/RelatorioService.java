package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.RelatorioDtos.CustoMaquinaItemResponse;
import com.fablab.financeiro.dto.RelatorioDtos.DoacoesDespesasMes;
import com.fablab.financeiro.dto.RelatorioDtos.DoacoesDespesasResponse;
import com.fablab.financeiro.dto.RelatorioDtos.DreResponse;
import com.fablab.financeiro.dto.RelatorioDtos.FluxoCaixaResponse;
import com.fablab.financeiro.dto.RelatorioDtos.FluxoCaixaSemana;
import com.fablab.financeiro.dto.RelatorioDtos.InadimplenciaItemResponse;
import com.fablab.financeiro.dto.RelatorioDtos.LucratividadeItemResponse;
import com.fablab.financeiro.entity.CustoEncomenda;
import com.fablab.financeiro.entity.DoacaoRecurso;
import com.fablab.financeiro.entity.LancamentoFinanceiro;
import com.fablab.financeiro.entity.StatusLancamento;
import com.fablab.financeiro.entity.TipoDoacao;
import com.fablab.financeiro.entity.TipoLancamento;
import com.fablab.financeiro.repository.CustoEncomendaRepository;
import com.fablab.financeiro.repository.DoacaoRecursoRepository;
import com.fablab.financeiro.repository.FechamentoEncomendaRepository;
import com.fablab.financeiro.repository.LancamentoFinanceiroRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relatórios de saúde financeira (D-2: shapes servidos; front nunca deriva).
 * Período via {@code dataInicio}/{@code dataFim} ou {@code periodo}
 * ({@code yyyy-MM}) — D-4 assumido.
 */
@Service
public class RelatorioService {

    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final DoacaoRecursoRepository doacaoRepository;
    private final CustoEncomendaRepository custoRepository;

    public RelatorioService(LancamentoFinanceiroRepository lancamentoRepository,
                            DoacaoRecursoRepository doacaoRepository,
                            CustoEncomendaRepository custoRepository,
                            FechamentoEncomendaRepository fechamentoRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.doacaoRepository = doacaoRepository;
        this.custoRepository = custoRepository;
    }

    /** Resolve o período: dataInicio/dataFim têm precedência sobre periodo (yyyy-MM). */
    public record Periodo(LocalDate inicio, LocalDate fim) {
    }

    public Periodo resolverPeriodo(LocalDate dataInicio, LocalDate dataFim, String periodo) {
        if (dataInicio != null || dataFim != null) {
            return new Periodo(dataInicio, dataFim);
        }
        if (periodo != null && !periodo.isBlank()) {
            YearMonth mes = YearMonth.parse(periodo.trim(), DateTimeFormatter.ofPattern("yyyy-MM"));
            return new Periodo(mes.atDay(1), mes.atEndOfMonth());
        }
        return new Periodo(null, null);
    }

    private List<LancamentoFinanceiro> noPeriodo(List<LancamentoFinanceiro> todos, Periodo periodo) {
        return todos.stream()
                .filter(l -> periodo.inicio() == null || !l.getDataVencimento().isBefore(periodo.inicio()))
                .filter(l -> periodo.fim() == null || !l.getDataVencimento().isAfter(periodo.fim()))
                .toList();
    }

    private BigDecimal somar(List<LancamentoFinanceiro> lista, TipoLancamento tipo) {
        return lista.stream()
                .filter(l -> l.getTipo() == tipo && l.getStatus() != StatusLancamento.CANCELADO)
                .map(LancamentoFinanceiro::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public FluxoCaixaResponse fluxoCaixa(LocalDate dataInicio, LocalDate dataFim, String periodo) {
        Periodo per = resolverPeriodo(dataInicio, dataFim, periodo);
        List<LancamentoFinanceiro> lista = noPeriodo(lancamentoRepository.findAll(), per);
        BigDecimal entradas = somar(lista, TipoLancamento.ENTRADA);
        BigDecimal saidas = somar(lista, TipoLancamento.SAIDA);

        Map<String, List<LancamentoFinanceiro>> porSemana = lista.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getDataVencimento().format(DateTimeFormatter.ofPattern("yyyy-'S'w")),
                        LinkedHashMap::new, Collectors.toList()));
        List<FluxoCaixaSemana> semanas = porSemana.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    BigDecimal en = somar(e.getValue(), TipoLancamento.ENTRADA);
                    BigDecimal sa = somar(e.getValue(), TipoLancamento.SAIDA);
                    return new FluxoCaixaSemana(e.getKey(), en, sa, en.subtract(sa));
                }).toList();
        return new FluxoCaixaResponse(entradas, saidas, entradas.subtract(saidas), semanas);
    }

    @Transactional(readOnly = true)
    public DreResponse dre(LocalDate dataInicio, LocalDate dataFim, String periodo) {
        Periodo per = resolverPeriodo(dataInicio, dataFim, periodo);
        List<LancamentoFinanceiro> lista = noPeriodo(lancamentoRepository.findAll(), per);
        BigDecimal entradas = somar(lista, TipoLancamento.ENTRADA);
        BigDecimal despesas = somar(lista, TipoLancamento.SAIDA);
        BigDecimal doacoes = doacaoRepository.findAll().stream()
                .filter(d -> per.inicio() == null || !d.getDataRecebimento().isBefore(per.inicio()))
                .filter(d -> per.fim() == null || !d.getDataRecebimento().isAfter(per.fim()))
                .map(DoacaoRecurso::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DreResponse(entradas, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                despesas, doacoes, entradas.add(doacoes).subtract(despesas));
    }

    @Transactional(readOnly = true)
    public List<LucratividadeItemResponse> lucratividade() {
        return custoRepository.findAll().stream()
                .collect(Collectors.toMap(CustoEncomenda::getIdEncomenda, c -> c,
                        (a, b) -> a.getDataCalculo().isAfter(b.getDataCalculo()) ? a : b))
                .values().stream()
                .sorted(Comparator.comparing(CustoEncomenda::getIdEncomenda))
                .map(c -> {
                    BigDecimal pct = c.getValorVenda().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                            : c.getMargemLucro().multiply(BigDecimal.valueOf(100))
                                    .divide(c.getValorVenda(), 2, RoundingMode.HALF_UP);
                    return new LucratividadeItemResponse(c.getIdEncomenda(), c.getValorVenda(),
                            c.getCustoTotal(), c.getMargemLucro(), pct);
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<InadimplenciaItemResponse> inadimplencia() {
        LocalDate hoje = LocalDate.now();
        return lancamentoRepository
                .findByStatusInAndDataVencimentoBefore(List.of(StatusLancamento.ATRASADO), hoje.plusDays(1))
                .stream()
                .filter(l -> l.getTipo() == TipoLancamento.ENTRADA)
                .map(l -> new InadimplenciaItemResponse(l.getId(), l.getIdReferenciaExterna(),
                        l.getValor(), l.getDataVencimento(),
                        ChronoUnit.DAYS.between(l.getDataVencimento(), hoje)))
                .toList();
    }

    @Transactional(readOnly = true)
    public DoacoesDespesasResponse doacoesDespesas(LocalDate dataInicio, LocalDate dataFim, String periodo) {
        Periodo per = resolverPeriodo(dataInicio, dataFim, periodo);
        List<DoacaoRecurso> doacoes = doacaoRepository.findAll().stream()
                .filter(d -> per.inicio() == null || !d.getDataRecebimento().isBefore(per.inicio()))
                .filter(d -> per.fim() == null || !d.getDataRecebimento().isAfter(per.fim()))
                .toList();
        BigDecimal totalDoacoes = doacoes.stream()
                .filter(d -> d.getTipo() == TipoDoacao.DOACAO)
                .map(DoacaoRecurso::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recursos = doacoes.stream()
                .filter(d -> d.getTipo() == TipoDoacao.PROJETO)
                .map(DoacaoRecurso::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal despesas = somar(noPeriodo(lancamentoRepository.findAll(), per), TipoLancamento.SAIDA);
        BigDecimal saldo = totalDoacoes.add(recursos).subtract(despesas);
        BigDecimal cobertura = despesas.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : totalDoacoes.add(recursos).multiply(BigDecimal.valueOf(100))
                        .divide(despesas, 2, RoundingMode.HALF_UP);

        Map<String, List<DoacaoRecurso>> porMes = doacoes.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getDataRecebimento().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, BigDecimal> despesasPorMes = noPeriodo(lancamentoRepository.findAll(), per).stream()
                .filter(l -> l.getTipo() == TipoLancamento.SAIDA && l.getStatus() != StatusLancamento.CANCELADO)
                .collect(Collectors.groupingBy(
                        l -> l.getDataVencimento().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, LancamentoFinanceiro::getValor, BigDecimal::add)));
        List<DoacoesDespesasMes> meses = new ArrayList<>();
        for (String mes : porMes.keySet().stream().sorted().toList()) {
            BigDecimal doa = porMes.get(mes).stream()
                    .map(DoacaoRecurso::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal des = despesasPorMes.getOrDefault(mes, BigDecimal.ZERO);
            meses.add(new DoacoesDespesasMes(mes, doa, des, doa.subtract(des)));
        }
        return new DoacoesDespesasResponse(totalDoacoes, recursos, despesas, saldo, cobertura, meses);
    }

    /**
     * Custo agregado por máquina via {@code idReferenciaExterna} dos
     * lançamentos de saída (convenção documentada).
     */
    @Transactional(readOnly = true)
    public List<CustoMaquinaItemResponse> custoMaquina() {
        Map<String, BigDecimal> porMaquina = lancamentoRepository.findAll().stream()
                .filter(l -> l.getTipo() == TipoLancamento.SAIDA
                        && l.getStatus() != StatusLancamento.CANCELADO
                        && l.getIdReferenciaExterna() != null)
                .collect(Collectors.groupingBy(LancamentoFinanceiro::getIdReferenciaExterna,
                        Collectors.reducing(BigDecimal.ZERO, LancamentoFinanceiro::getValor, BigDecimal::add)));
        BigDecimal total = porMaquina.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return porMaquina.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new CustoMaquinaItemResponse(e.getKey(), e.getValue(),
                        total.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                                : e.getValue().multiply(BigDecimal.valueOf(100))
                                        .divide(total, 2, RoundingMode.HALF_UP)))
                .toList();
    }
}
