package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.ParametroOverheadRequest;
import com.fablab.financeiro.dto.ParametroOverheadResponse;
import com.fablab.financeiro.service.ParametroOverheadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Parâmetros de taxa de overhead para o custeio por ordem.
 */
@RestController
@RequestMapping("/parametros-overhead")
@PreAuthorize("hasRole('ADMIN')")
public class ParametroOverheadController {

    private final ParametroOverheadService overheadService;

    public ParametroOverheadController(ParametroOverheadService overheadService) {
        this.overheadService = overheadService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParametroOverheadResponse definir(@Valid @RequestBody ParametroOverheadRequest request) {
        return overheadService.definir(request);
    }
}