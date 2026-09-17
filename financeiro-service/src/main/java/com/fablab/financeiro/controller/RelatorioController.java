package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.CustoMaquinaItemResponse;
import com.fablab.financeiro.dto.DoacoesDespesasResponse;
import com.fablab.financeiro.dto.DreResponse;
import com.fablab.financeiro.dto.FluxoCaixaResponse;
import com.fablab.financeiro.dto.InadimplenciaItemResponse;
import com.fablab.financeiro.dto.LucratividadeItemResponse;
import com.fablab.financeiro.service.RelatorioService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Relatórios financeiros do Fab Lab.
 */
@RestController
@RequestMapping("/relatorios")
@PreAuthorize("hasRole('ADMIN')")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/fluxo-caixa")
    public FluxoCaixaResponse fluxoCaixa(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return relatorioService.fluxoCaixa(dataInicio, dataFim);
    }

    @GetMapping("/dre")
    public DreResponse dre(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return relatorioService.dre(dataInicio, dataFim);
    }

    @GetMapping("/lucratividade")
    public List<LucratividadeItemResponse> lucratividade() {
        return relatorioService.lucratividade();
    }

    @GetMapping("/inadimplencia")
    public List<InadimplenciaItemResponse> inadimplencia() {
        return relatorioService.inadimplencia();
    }

    @GetMapping("/doacoes-despesas")
    public DoacoesDespesasResponse doacoesDespesas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return relatorioService.doacoesDespesas(dataInicio, dataFim);
    }

    @GetMapping("/custo-maquina")
    public List<CustoMaquinaItemResponse> custoMaquina() {
        return relatorioService.custoMaquina();
    }
}