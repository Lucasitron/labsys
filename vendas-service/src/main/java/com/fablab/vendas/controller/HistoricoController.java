package com.fablab.vendas.controller;

import com.fablab.vendas.dto.EncomendaDtos.HistoricoResponse;
import com.fablab.vendas.service.HistoricoService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Histórico de movimentações de uma encomenda. */
@RestController
@RequestMapping("/historico-status")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping("/{idEncomenda}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public ResponseEntity<List<HistoricoResponse>> listar(@PathVariable Long idEncomenda) {
        return ResponseEntity.ok(historicoService.listar(idEncomenda));
    }
}
