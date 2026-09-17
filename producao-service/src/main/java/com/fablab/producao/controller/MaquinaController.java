package com.fablab.producao.controller;

import com.fablab.producao.dto.HistoricoUsoResponse;
import com.fablab.producao.dto.MaquinaRequest;
import com.fablab.producao.dto.MaquinaResponse;
import com.fablab.producao.dto.MaquinaStatusRequest;
import com.fablab.producao.dto.UsoMaquinaFimRequest;
import com.fablab.producao.dto.UsoMaquinaRequest;
import com.fablab.producao.entity.MaquinaStatus;
import com.fablab.producao.service.MaquinaService;
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

/** Endpoints de máquinas e histórico de uso. */
@RestController
@RequestMapping("/maquinas")
public class MaquinaController {

    private final MaquinaService maquinaService;

    public MaquinaController(MaquinaService maquinaService) {
        this.maquinaService = maquinaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<MaquinaResponse> criar(@Valid @RequestBody MaquinaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maquinaService.criar(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public List<MaquinaResponse> listar(@RequestParam(required = false) MaquinaStatus status) {
        return maquinaService.listar(status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public MaquinaResponse buscar(@PathVariable Long id) {
        return maquinaService.buscar(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public MaquinaResponse atualizar(@PathVariable Long id, @Valid @RequestBody MaquinaRequest request) {
        return maquinaService.atualizar(id, request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public MaquinaResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody MaquinaStatusRequest request) {
        return maquinaService.alterarStatus(id, request);
    }

    @PostMapping("/{id}/uso")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<HistoricoUsoResponse> iniciarUso(@PathVariable Long id,
                                                           @Valid @RequestBody UsoMaquinaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maquinaService.iniciarUso(id, request));
    }

    @PutMapping("/{id}/uso/{idUso}/fim")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public HistoricoUsoResponse encerrarUso(@PathVariable Long id,
                                            @PathVariable Long idUso,
                                            @RequestBody UsoMaquinaFimRequest request) {
        return maquinaService.encerrarUso(id, idUso, request);
    }

    @GetMapping("/{id}/historico")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public List<HistoricoUsoResponse> historico(@PathVariable Long id) {
        return maquinaService.historico(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        maquinaService.remover(id);
        return ResponseEntity.noContent().build();
    }
}