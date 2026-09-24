package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.LancamentoDtos.LancamentoListaResponse;
import com.fablab.financeiro.dto.LancamentoDtos.LancamentoRequest;
import com.fablab.financeiro.dto.LancamentoDtos.LancamentoResponse;
import com.fablab.financeiro.dto.LancamentoDtos.PagamentoRequest;
import com.fablab.financeiro.entity.StatusLancamento;
import com.fablab.financeiro.entity.TipoLancamento;
import com.fablab.financeiro.service.LancamentoFinanceiroService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de lançamentos financeiros (contas a pagar/receber). */
@RestController
@RequestMapping("/lancamentos")
@PreAuthorize("hasRole('ADMIN')")
public class LancamentoController {

    private final LancamentoFinanceiroService service;

    public LancamentoController(LancamentoFinanceiroService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<LancamentoResponse> criar(@Valid @RequestBody LancamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @GetMapping
    public ResponseEntity<LancamentoListaResponse> listar(
            @RequestParam(required = false) StatusLancamento status,
            @RequestParam(required = false) TipoLancamento tipo,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String origem,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimentoDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimentoAte) {
        return ResponseEntity.ok(service.listar(status, tipo, idCategoria, dataInicio, dataFim,
                origem, vencimentoDe, vencimentoAte));
    }

    @PutMapping("/{id}/pagamento")
    public ResponseEntity<LancamentoResponse> pagar(@PathVariable Long id,
                                                    @Valid @RequestBody(required = false) PagamentoRequest request) {
        return ResponseEntity.ok(service.registrarPagamento(id, request));
    }
}
