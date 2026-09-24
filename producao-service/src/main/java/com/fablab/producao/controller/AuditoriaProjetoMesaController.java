package com.fablab.producao.controller;

import com.fablab.producao.dto.AuditoriaProjetoMesaRequest;
import com.fablab.producao.dto.AuditoriaProjetoMesaResponse;
import com.fablab.producao.service.ProjetoMesaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de auditoria de projetos de mesa (exclusivo do Admin). */
@RestController
@RequestMapping("/auditorias-projeto-mesa")
public class AuditoriaProjetoMesaController {

    private final ProjetoMesaService projetoMesaService;

    public AuditoriaProjetoMesaController(ProjetoMesaService projetoMesaService) {
        this.projetoMesaService = projetoMesaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditoriaProjetoMesaResponse> auditar(
            @Valid @RequestBody AuditoriaProjetoMesaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetoMesaService.auditar(request));
    }

    @GetMapping("/{idProjetoMesa}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public List<AuditoriaProjetoMesaResponse> listar(@PathVariable Long idProjetoMesa) {
        return projetoMesaService.listarAuditorias(idProjetoMesa);
    }
}