package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.FinanceiroPrincipal;
import com.fablab.financeiro.dto.ParametroDtos.OverheadRequest;
import com.fablab.financeiro.dto.ParametroDtos.OverheadResponse;
import com.fablab.financeiro.dto.ParametroDtos.ValorHoraRequest;
import com.fablab.financeiro.dto.ParametroDtos.ValorHoraResponse;
import com.fablab.financeiro.service.ParametroService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de valores/hora por nível e taxa de overhead. */
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class ParametroController {

    private final ParametroService service;

    public ParametroController(ParametroService service) {
        this.service = service;
    }

    @PostMapping("/valores-hora")
    public ResponseEntity<ValorHoraResponse> definirValorHora(
            @Valid @RequestBody ValorHoraRequest request,
            @AuthenticationPrincipal FinanceiroPrincipal principal) {
        Long idUsuario = principal != null ? principal.idPessoa() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(service.definirValorHora(request, idUsuario));
    }

    @GetMapping("/valores-hora")
    public ResponseEntity<List<ValorHoraResponse>> valoresVigentes() {
        return ResponseEntity.ok(service.valoresVigentes());
    }

    @PostMapping("/parametros-overhead")
    public ResponseEntity<OverheadResponse> definirOverhead(
            @Valid @RequestBody OverheadRequest request,
            @AuthenticationPrincipal FinanceiroPrincipal principal) {
        Long idUsuario = principal != null ? principal.idPessoa() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(service.definirOverhead(request, idUsuario));
    }

    @GetMapping("/parametros-overhead")
    public ResponseEntity<OverheadResponse> overheadVigente() {
        return ResponseEntity.ok(service.overheadVigente());
    }
}
