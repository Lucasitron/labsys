package com.fablab.vendas.controller;

import com.fablab.vendas.dto.CrmDtos.TarefaAtualizacaoRequest;
import com.fablab.vendas.dto.CrmDtos.TarefaListaResponse;
import com.fablab.vendas.dto.CrmDtos.TarefaRequest;
import com.fablab.vendas.dto.CrmDtos.TarefaResponse;
import com.fablab.vendas.dto.VendasPrincipal;
import com.fablab.vendas.service.CrmService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de tarefas de marketing. */
@RestController
@RequestMapping("/tarefas-marketing")
public class TarefaMarketingController {

    private static final String ACESSO = "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";

    private final CrmService crmService;

    public TarefaMarketingController(CrmService crmService) {
        this.crmService = crmService;
    }

    @PostMapping
    @PreAuthorize(ACESSO)
    public ResponseEntity<TarefaResponse> criar(@Valid @RequestBody TarefaRequest request,
                                                @AuthenticationPrincipal VendasPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crmService.criarTarefa(request, principal));
    }

    @GetMapping
    @PreAuthorize(ACESSO)
    public ResponseEntity<TarefaListaResponse> listar(
            @RequestParam(required = false) Long responsavelId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(crmService.listarTarefas(responsavelId, status));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ACESSO)
    public ResponseEntity<TarefaResponse> atualizar(@PathVariable Long id,
                                                    @Valid @RequestBody TarefaAtualizacaoRequest request,
                                                    @AuthenticationPrincipal VendasPrincipal principal) {
        return ResponseEntity.ok(crmService.atualizarTarefa(id, request, principal));
    }
}
