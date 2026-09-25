package com.fablab.rh.controller;

import com.fablab.rh.dto.AvaliarCandidatoRequest;
import com.fablab.rh.dto.EstagioRequest;
import com.fablab.rh.dto.GrupoProcessoRequest;
import com.fablab.rh.dto.GrupoProcessoResponse;
import com.fablab.rh.dto.ProcessoSeletivoListaResponse;
import com.fablab.rh.dto.ProcessoSeletivoRequest;
import com.fablab.rh.dto.ProcessoSeletivoResponse;
import com.fablab.rh.dto.ProcessoSeletivoStatusRequest;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.entity.StatusProcesso;
import com.fablab.rh.service.ProcessoSeletivoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO')")
    public ResponseEntity<ProcessoSeletivoListaResponse> listar(
            @RequestParam(required = false) StatusProcesso estagio,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(processoService.listar(estagio, principal));
    }

    @PostMapping("/grupos")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO')")
    public ResponseEntity<GrupoProcessoResponse> criarGrupo(
            @Valid @RequestBody GrupoProcessoRequest request,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(processoService.criarGrupo(request, principal));
    }

    @PatchMapping("/{id}/estagio")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO')")
    public ResponseEntity<ProcessoSeletivoResponse> moverEstagio(
            @PathVariable Long id,
            @Valid @RequestBody EstagioRequest request,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(processoService.moverEstagio(id, request, principal));
    }

    @PostMapping("/{id}/membros/{pessoaId}/avaliar")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO')")
    public ResponseEntity<ProcessoSeletivoResponse> avaliar(
            @PathVariable Long id,
            @PathVariable Long pessoaId,
            @Valid @RequestBody AvaliarCandidatoRequest request,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(processoService.avaliar(id, pessoaId, request, principal));
    }
}