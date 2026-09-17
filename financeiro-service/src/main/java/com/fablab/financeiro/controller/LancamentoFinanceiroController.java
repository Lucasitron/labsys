package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.LancamentoFinanceiroRequest;
import com.fablab.financeiro.dto.LancamentoFinanceiroResponse;
import com.fablab.financeiro.entity.StatusLancamento;
import com.fablab.financeiro.service.LancamentoFinanceiroService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contas a pagar/receber: criação, listagem e liquidação de lançamentos.
 */
@RestController
@RequestMapping("/lancamentos")
@PreAuthorize("hasRole('ADMIN')")
public class LancamentoFinanceiroController {

    private final LancamentoFinanceiroService lancamentoService;

    public LancamentoFinanceiroController(LancamentoFinanceiroService lancamentoService) {
        this.lancamentoService = lancamentoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoFinanceiroResponse criar(@Valid @RequestBody LancamentoFinanceiroRequest request) {
        return lancamentoService.criar(request);
    }

    @GetMapping
    public List<LancamentoFinanceiroResponse> listar(
            @RequestParam(required = false) StatusLancamento status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long idCategoria) {
        return lancamentoService.listar(status, dataInicio, dataFim, idCategoria);
    }

    @PutMapping("/{idLancamento}/pagamento")
    public LancamentoFinanceiroResponse liquidar(@PathVariable Long idLancamento) {
        return lancamentoService.registrarPagamento(idLancamento);
    }
}