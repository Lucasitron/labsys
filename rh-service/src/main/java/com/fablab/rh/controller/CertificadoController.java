package com.fablab.rh.controller;

import com.fablab.rh.dto.CertificadoEmitidoResponse;
import com.fablab.rh.dto.DecisaoSolicitacaoRequest;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.dto.SolicitacaoCertificadoResponse;
import com.fablab.rh.dto.SolicitarCertificadoRequest;
import com.fablab.rh.entity.StatusSolicitacao;
import com.fablab.rh.service.CertificadoService;
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
 * Endpoints de solicitação, decisão e consulta de certificados de horas.
 */
@RestController
@RequestMapping("/certificados")
public class CertificadoController {

    private final CertificadoService certificadoService;

    public CertificadoController(CertificadoService certificadoService) {
        this.certificadoService = certificadoService;
    }

    @PostMapping("/solicitar")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO', 'ESTAGIARIO')")
    public ResponseEntity<SolicitacaoCertificadoResponse> solicitar(
            @Valid @RequestBody SolicitarCertificadoRequest request,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certificadoService.solicitar(request, principal));
    }

    @GetMapping("/solicitacoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO', 'ESTAGIARIO')")
    public ResponseEntity<List<SolicitacaoCertificadoResponse>> listarSolicitacoes(
            @RequestParam(required = false) StatusSolicitacao status,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(certificadoService.listarSolicitacoes(status, principal));
    }

    @PutMapping("/solicitacoes/{id}/aprovar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CertificadoEmitidoResponse> aprovar(
            @PathVariable Long id,
            @RequestBody(required = false) DecisaoSolicitacaoRequest request,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(certificadoService.aprovar(id, request, principal));
    }

    @PutMapping("/solicitacoes/{id}/rejeitar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SolicitacaoCertificadoResponse> rejeitar(
            @PathVariable Long id,
            @RequestBody(required = false) DecisaoSolicitacaoRequest request,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(certificadoService.rejeitar(id, request, principal));
    }

    @GetMapping("/emitidos")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO', 'ESTAGIARIO')")
    public ResponseEntity<List<CertificadoEmitidoResponse>> listarEmitidos(
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(certificadoService.listarEmitidos(principal));
    }

    @GetMapping("/emitidos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO', 'ESTAGIARIO')")
    public ResponseEntity<CertificadoEmitidoResponse> obterEmitido(
            @PathVariable Long id,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(certificadoService.obterEmitido(id, principal));
    }
}