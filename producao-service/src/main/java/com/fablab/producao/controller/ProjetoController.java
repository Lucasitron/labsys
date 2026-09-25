package com.fablab.producao.controller;

import com.fablab.producao.dto.ProjetoRequest;
import com.fablab.producao.dto.ProjetoResponse;
import com.fablab.producao.dto.ProjetoStatusRequest;
import com.fablab.producao.entity.ProjetoStatus;
import com.fablab.producao.service.ProjetoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de projetos. */
@RestController
@RequestMapping("/projetos")
public class ProjetoController {

    private final ProjetoService projetoService;

    public ProjetoController(ProjetoService projetoService) {
        this.projetoService = projetoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<ProjetoResponse> criar(@Valid @RequestBody ProjetoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetoService.criar(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public List<ProjetoResponse> listar(@RequestParam(required = false) ProjetoStatus status,
                                        @RequestParam(required = false) Long idResponsavel) {
        return projetoService.listar(status, idResponsavel);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public ProjetoResponse buscar(@PathVariable Long id) {
        return projetoService.buscar(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ProjetoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProjetoRequest request) {
        return projetoService.atualizar(id, request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ProjetoResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody ProjetoStatusRequest request) {
        return projetoService.alterarStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        projetoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}