package com.fablab.rh.controller;

import com.fablab.rh.dto.FuncionarioHorasResponse;
import com.fablab.rh.dto.FuncionarioRequest;
import com.fablab.rh.dto.FuncionarioResponse;
import com.fablab.rh.dto.NivelAlteradoResponse;
import com.fablab.rh.dto.NivelRequest;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.service.FuncionarioService;
import jakarta.validation.Valid;
import java.util.List;
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
 * Endpoints de gestão de funcionários.
 */
@RestController
@RequestMapping("/funcionarios")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    public FuncionarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FuncionarioResponse>> listar(
            @RequestParam(required = false) NivelAcesso nivel,
            @RequestParam(required = false) String departamento) {
        return ResponseEntity.ok(funcionarioService.listar(nivel, departamento));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuncionarioResponse> vincular(@Valid @RequestBody FuncionarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funcionarioService.vincular(request));
    }

    @PutMapping("/{id}/nivel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NivelAlteradoResponse> alterarNivel(@PathVariable Long id,
                                                              @Valid @RequestBody NivelRequest request,
                                                              @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(funcionarioService.alterarNivel(id, request, principal));
    }

    @GetMapping("/{id}/horas")
    public ResponseEntity<FuncionarioHorasResponse> totalHoras(@PathVariable Long id,
                                                               @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(funcionarioService.totalHoras(id, principal));
    }
}