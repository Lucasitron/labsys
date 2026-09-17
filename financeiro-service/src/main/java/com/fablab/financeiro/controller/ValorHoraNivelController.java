package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.ValorHoraNivelRequest;
import com.fablab.financeiro.dto.ValorHoraNivelResponse;
import com.fablab.financeiro.service.ValorHoraNivelService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Valor da hora de trabalho por nível de acesso.
 */
@RestController
@RequestMapping("/valores-hora")
@PreAuthorize("hasRole('ADMIN')")
public class ValorHoraNivelController {

    private final ValorHoraNivelService valorHoraService;

    public ValorHoraNivelController(ValorHoraNivelService valorHoraService) {
        this.valorHoraService = valorHoraService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ValorHoraNivelResponse definir(@Valid @RequestBody ValorHoraNivelRequest request) {
        return valorHoraService.definir(request);
    }

    @GetMapping
    public List<ValorHoraNivelResponse> listarVigentes() {
        return valorHoraService.listarVigentes();
    }
}