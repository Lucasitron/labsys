package com.fablab.rh.controller;

import com.fablab.rh.dto.AvaliacaoRequest;
import com.fablab.rh.dto.AvaliacaoResponse;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.dto.TreinamentoRequest;
import com.fablab.rh.dto.TreinamentoResponse;
import com.fablab.rh.service.TreinamentoService;
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

/**
 * Endpoints do módulo de treinamento (LMS).
 */
@RestController
@RequestMapping("/treinamentos")
public class TreinamentoController {

    private final TreinamentoService treinamentoService;

    public TreinamentoController(TreinamentoService treinamentoService) {
        this.treinamentoService = treinamentoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO')")
    public ResponseEntity<TreinamentoResponse> criar(@Valid @RequestBody TreinamentoRequest request,
                                                     @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(treinamentoService.criar(request, principal));
    }

    @PostMapping("/{id}/avaliacoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO')")
    public ResponseEntity<AvaliacaoResponse> avaliar(@PathVariable Long id,
                                                     @Valid @RequestBody AvaliacaoRequest request,
                                                     @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(treinamentoService.avaliar(id, request, principal));
    }

    @GetMapping("/{id}/avaliacoes")
    public ResponseEntity<List<AvaliacaoResponse>> listarAvaliacoes(@PathVariable Long id) {
        return ResponseEntity.ok(treinamentoService.listarAvaliacoes(id));
    }
}