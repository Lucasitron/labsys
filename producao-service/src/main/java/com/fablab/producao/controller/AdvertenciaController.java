package com.fablab.producao.controller;

import com.fablab.producao.dto.AdvertenciaRequest;
import com.fablab.producao.dto.AdvertenciaResponse;
import com.fablab.producao.service.AdvertenciaService;
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

/** Endpoints de advertências. */
@RestController
@RequestMapping("/advertencias")
public class AdvertenciaController {

    private final AdvertenciaService advertenciaService;

    public AdvertenciaController(AdvertenciaService advertenciaService) {
        this.advertenciaService = advertenciaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdvertenciaResponse> registrar(@Valid @RequestBody AdvertenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(advertenciaService.registrar(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public List<AdvertenciaResponse> listar() {
        return advertenciaService.listar(null);
    }

    @GetMapping("/{idFuncionario}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public List<AdvertenciaResponse> listarPorFuncionario(@PathVariable Long idFuncionario) {
        return advertenciaService.listar(idFuncionario);
    }
}