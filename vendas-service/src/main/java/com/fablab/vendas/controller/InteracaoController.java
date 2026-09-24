package com.fablab.vendas.controller;

import com.fablab.vendas.dto.CrmDtos.InteracaoRequest;
import com.fablab.vendas.dto.CrmDtos.InteracaoResponse;
import com.fablab.vendas.dto.VendasPrincipal;
import com.fablab.vendas.service.CrmService;
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

/** Endpoints de interações com clientes (timeline no detalhe). */
@RestController
@RequestMapping("/interacoes")
public class InteracaoController {

    private static final String ACESSO = "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";

    private final CrmService crmService;

    public InteracaoController(CrmService crmService) {
        this.crmService = crmService;
    }

    @PostMapping
    @PreAuthorize(ACESSO)
    public ResponseEntity<InteracaoResponse> registrar(@Valid @RequestBody InteracaoRequest request,
                                                       @AuthenticationPrincipal VendasPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(crmService.registrarInteracao(request, principal));
    }

    @GetMapping("/{idCliente}")
    @PreAuthorize(ACESSO)
    public ResponseEntity<List<InteracaoResponse>> listar(@PathVariable Long idCliente) {
        return ResponseEntity.ok(crmService.listarInteracoes(idCliente));
    }
}
