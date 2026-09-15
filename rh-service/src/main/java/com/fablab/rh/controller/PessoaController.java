package com.fablab.rh.controller;

import com.fablab.rh.dto.PessoaRequest;
import com.fablab.rh.dto.PessoaResponse;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.service.PessoaService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestão de pessoas.
 */
@RestController
@RequestMapping("/pessoas")
public class PessoaController {

    private final PessoaService pessoaService;

    public PessoaController(PessoaService pessoaService) {
        this.pessoaService = pessoaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO')")
    public ResponseEntity<PessoaResponse> cadastrar(@Valid @RequestBody PessoaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pessoaService.cadastrar(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PessoaResponse> buscar(@PathVariable Long id,
                                                 @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(pessoaService.buscar(id, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO')")
    public ResponseEntity<PessoaResponse> atualizar(@PathVariable Long id,
                                                    @Valid @RequestBody PessoaRequest request,
                                                    @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(pessoaService.atualizar(id, request, principal));
    }
}