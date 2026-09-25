package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.RelatorioDtos.CustoMaquinaItemResponse;
import com.fablab.financeiro.dto.RelatorioDtos.DoacoesDespesasResponse;
import com.fablab.financeiro.dto.RelatorioDtos.DreResponse;
import com.fablab.financeiro.dto.RelatorioDtos.FluxoCaixaResponse;
import com.fablab.financeiro.dto.RelatorioDtos.InadimplenciaItemResponse;
import com.fablab.financeiro.dto.RelatorioDtos.LucratividadeItemResponse;
import com.fablab.financeiro.service.RelatorioService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints dos 6 relatórios de saúde financeira (shapes servidos — D-2). */
@RestController
@RequestMapping("/relatorios")
@PreAuthorize("hasRole('ADMIN')")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @GetMapping("/fluxo-caixa")
    public ResponseEntity<FluxoCaixaResponse> fluxoCaixa(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String periodo) {
        return ResponseEntity.ok(service.fluxoCaixa(dataInicio, dataFim, periodo));
    }

    @GetMapping("/dre")
    public ResponseEntity<DreResponse> dre(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String periodo) {
        return ResponseEntity.ok(service.dre(dataInicio, dataFim, periodo));
    }

    @GetMapping("/lucratividade")
    public ResponseEntity<List<LucratividadeItemResponse>> lucratividade() {
        return ResponseEntity.ok(service.lucratividade());
    }

    @GetMapping("/inadimplencia")
    public ResponseEntity<List<InadimplenciaItemResponse>> inadimplencia() {
        return ResponseEntity.ok(service.inadimplencia());
    }

    @GetMapping("/doacoes-despesas")
    public ResponseEntity<DoacoesDespesasResponse> doacoesDespesas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String periodo) {
        return ResponseEntity.ok(service.doacoesDespesas(dataInicio, dataFim, periodo));
    }

    @GetMapping("/custo-maquina")
    public ResponseEntity<List<CustoMaquinaItemResponse>> custoMaquina() {
        return ResponseEntity.ok(service.custoMaquina());
    }
}
