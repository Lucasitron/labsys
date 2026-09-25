package com.fablab.producao.controller;

import com.fablab.producao.dto.Parametro5SRequest;
import com.fablab.producao.dto.Parametro5SResponse;
import com.fablab.producao.service.Parametro5SService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de parâmetros 5S (alteração exclusiva do Admin). */
@RestController
@RequestMapping("/parametros-5s")
public class Parametro5SController {

    private final Parametro5SService parametroService;

    public Parametro5SController(Parametro5SService parametroService) {
        this.parametroService = parametroService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public List<Parametro5SResponse> listar() {
        return parametroService.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public Parametro5SResponse buscar(@PathVariable Long id) {
        return parametroService.buscar(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Parametro5SResponse atualizar(@PathVariable Long id, @Valid @RequestBody Parametro5SRequest request) {
        return parametroService.atualizar(id, request);
    }
}