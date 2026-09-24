package com.fablab.rh.controller;

import com.fablab.rh.dto.ConviteRequest;
import com.fablab.rh.dto.FuncionarioResponse;
import com.fablab.rh.dto.NivelAlteradoResponse;
import com.fablab.rh.dto.NivelMembroRequest;
import com.fablab.rh.dto.NiveisResponse;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.service.NivelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Níveis de acesso (Admin): matriz de permissões, alteração de membro e
 * convites.
 */
@RestController
@RequestMapping("/niveis")
@PreAuthorize("hasRole('ADMIN')")
public class NivelController {

    private final NivelService nivelService;

    public NivelController(NivelService nivelService) {
        this.nivelService = nivelService;
    }

    @GetMapping
    public ResponseEntity<NiveisResponse> matriz() {
        return ResponseEntity.ok(nivelService.matriz());
    }

    @PatchMapping("/{id}/membros")
    public ResponseEntity<NivelAlteradoResponse> alterarMembro(
            @PathVariable Long id,
            @Valid @RequestBody NivelMembroRequest request,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(nivelService.alterarMembro(id, request, principal));
    }

    @PostMapping("/convites")
    public ResponseEntity<FuncionarioResponse> convidar(
            @Valid @RequestBody ConviteRequest request,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(nivelService.convidar(request, principal));
    }
}
