package com.fablab.rh.controller;

import com.fablab.rh.dto.HorasDisponiveisResponse;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.service.CertificadoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de horas disponíveis para certificado.
 */
@RestController
@RequestMapping("/horas")
public class HorasController {

    private final CertificadoService certificadoService;

    public HorasController(CertificadoService certificadoService) {
        this.certificadoService = certificadoService;
    }

    @GetMapping("/disponiveis")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO', 'ESTAGIARIO')")
    public ResponseEntity<HorasDisponiveisResponse> horasDisponiveis(
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(certificadoService.horasDisponiveis(principal));
    }
}