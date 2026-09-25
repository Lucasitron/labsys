package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.CusteioDtos.CustoResponse;
import com.fablab.financeiro.dto.CusteioDtos.FechamentoRequest;
import com.fablab.financeiro.dto.CusteioDtos.FechamentoResponse;
import com.fablab.financeiro.dto.FinanceiroPrincipal;
import com.fablab.financeiro.service.CusteioService;
import com.fablab.financeiro.service.FechamentoEncomendaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de fechamento de encomenda e consulta de custo (Job Order Costing). */
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class CusteioController {

    private final FechamentoEncomendaService fechamentoService;
    private final CusteioService custeioService;

    public CusteioController(FechamentoEncomendaService fechamentoService, CusteioService custeioService) {
        this.fechamentoService = fechamentoService;
        this.custeioService = custeioService;
    }

    @PostMapping("/fechamento-encomenda")
    public ResponseEntity<FechamentoResponse> fechar(
            @Valid @RequestBody FechamentoRequest request,
            @AuthenticationPrincipal FinanceiroPrincipal principal) {
        Long idUsuario = principal != null ? principal.idPessoa() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(fechamentoService.criar(request, idUsuario));
    }

    @GetMapping("/fechamento-encomenda")
    public ResponseEntity<List<FechamentoResponse>> listar() {
        return ResponseEntity.ok(fechamentoService.listar());
    }

    @GetMapping("/fechamento-encomenda/{idEncomenda}")
    public ResponseEntity<FechamentoResponse> consultar(@PathVariable Integer idEncomenda) {
        return ResponseEntity.ok(fechamentoService.consultar(idEncomenda));
    }

    /**
     * Alteração de encomenda (D-5): encerra a ordem atual e abre nova ordem
     * com novas estimativas e valor — o fechamento é imutável.
     */
    @PostMapping("/fechamento-encomenda/{idEncomenda}/nova-ordem")
    public ResponseEntity<FechamentoResponse> novaOrdem(
            @PathVariable Integer idEncomenda,
            @Valid @RequestBody FechamentoRequest request,
            @AuthenticationPrincipal FinanceiroPrincipal principal) {
        Long idUsuario = principal != null ? principal.idPessoa() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fechamentoService.novaOrdem(idEncomenda, request, idUsuario));
    }

    @GetMapping("/custos-encomenda/{idEncomenda}")
    public ResponseEntity<CustoResponse> custo(@PathVariable Integer idEncomenda) {
        return ResponseEntity.ok(custeioService.consultar(idEncomenda));
    }
}
