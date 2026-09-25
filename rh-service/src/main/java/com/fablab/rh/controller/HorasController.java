package com.fablab.rh.controller;

import com.fablab.rh.dto.ApontamentoHorasRequest;
import com.fablab.rh.dto.ApontamentoHorasResponse;
import com.fablab.rh.dto.HorasDisponiveisResponse;
import com.fablab.rh.dto.RejeicaoApontamentoRequest;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.dto.ValidacaoApontamentoRequest;
import com.fablab.rh.entity.StatusApontamento;
import com.fablab.rh.service.ApontamentoHorasService;
import com.fablab.rh.service.CertificadoService;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fachada de horas (contrato da tela de registro): lista, registra, valida e
 * rejeita — delegando ao serviço canônico de apontamentos.
 */
@RestController
@RequestMapping("/horas")
public class HorasController {

    private final CertificadoService certificadoService;
    private final ApontamentoHorasService apontamentoService;

    public HorasController(CertificadoService certificadoService,
                           ApontamentoHorasService apontamentoService) {
        this.certificadoService = certificadoService;
        this.apontamentoService = apontamentoService;
    }

    @GetMapping("/disponiveis")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO', 'ESTAGIARIO')")
    public ResponseEntity<HorasDisponiveisResponse> horasDisponiveis(
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(certificadoService.horasDisponiveis(principal));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO', 'ESTAGIARIO')")
    public ResponseEntity<List<ApontamentoHorasResponse>> listar(
            @RequestParam(required = false) StatusApontamento status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(apontamentoService.listar(status, periodo, principal));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO')")
    public ResponseEntity<ApontamentoHorasResponse> registrar(
            @Valid @RequestBody ApontamentoHorasRequest request,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apontamentoService.registrar(request, principal));
    }

    @PatchMapping("/{id}/validar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApontamentoHorasResponse> validar(
            @PathVariable Long id,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(apontamentoService.validar(
                id, new ValidacaoApontamentoRequest(StatusApontamento.VALIDADO, null), principal));
    }

    @PatchMapping("/{id}/rejeitar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApontamentoHorasResponse> rejeitar(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RejeicaoApontamentoRequest request,
            @AuthenticationPrincipal RhPrincipal principal) {
        String motivo = request == null ? null : request.motivo();
        return ResponseEntity.ok(apontamentoService.validar(
                id, new ValidacaoApontamentoRequest(StatusApontamento.REJEITADO, motivo), principal));
    }
}
