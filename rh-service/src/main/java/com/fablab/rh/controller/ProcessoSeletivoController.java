package com.fablab.rh.controller;

import com.fablab.rh.dto.ProcessoSeletivoRequest;
import com.fablab.rh.dto.ProcessoSeletivoResponse;
import com.fablab.rh.dto.ProcessoSeletivoStatusRequest;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.service.ProcessoSeletivoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints do processo seletivo.
 */
@RestController
@RequestMapping("/processo-seletivo")
public class ProcessoSeletivoController {

    private final ProcessoSeletivoService processoService;

    public ProcessoSeletivoController(ProcessoSeletivoService processoService) {
        this.processoService = processoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO')")
    public ResponseEntity<ProcessoSeletivoResponse> iniciar(
            @Valid @RequestBody ProcessoSeletivoRequest request,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(processoService.iniciar(request, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO')")
    public ResponseEntity<ProcessoSeletivoResponse> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProcessoSeletivoStatusRequest request,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(processoService.atualizarStatus(id, request, principal));
    }
}