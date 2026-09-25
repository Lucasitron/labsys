package com.fablab.rh.controller;

import com.fablab.rh.dto.ApontamentoHorasRequest;
import com.fablab.rh.dto.ApontamentoHorasResponse;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.dto.ValidacaoApontamentoRequest;
import com.fablab.rh.entity.StatusApontamento;
import com.fablab.rh.service.ApontamentoHorasService;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de apontamento de horas (encomenda/projeto).
 */
@RestController
@RequestMapping("/apontamentos-horas")
public class ApontamentoHorasController {

    private final ApontamentoHorasService apontamentoService;

    public ApontamentoHorasController(ApontamentoHorasService apontamentoService) {
        this.apontamentoService = apontamentoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO')")
    public ResponseEntity<ApontamentoHorasResponse> registrar(
            @Valid @RequestBody ApontamentoHorasRequest request,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apontamentoService.registrar(request, principal));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO', 'ESTAGIARIO')")
    public ResponseEntity<List<ApontamentoHorasResponse>> listar(
            @RequestParam(required = false) StatusApontamento status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(apontamentoService.listar(status, periodo, principal));
    }

    @PutMapping("/{id}/validar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApontamentoHorasResponse> validar(
            @PathVariable Long id,
            @Valid @RequestBody ValidacaoApontamentoRequest request,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(apontamentoService.validar(id, request, principal));
    }
}