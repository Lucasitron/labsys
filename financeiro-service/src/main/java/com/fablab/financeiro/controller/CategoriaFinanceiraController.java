package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.CategoriaFinanceiraRequest;
import com.fablab.financeiro.dto.CategoriaFinanceiraResponse;
import com.fablab.financeiro.entity.TipoCategoriaFinanceira;
import com.fablab.financeiro.service.CategoriaFinanceiraService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Categorias de lançamento (receitas/despesas).
 */
@RestController
@RequestMapping("/categorias")
@PreAuthorize("hasRole('ADMIN')")
public class CategoriaFinanceiraController {

    private final CategoriaFinanceiraService categoriaService;

    public CategoriaFinanceiraController(CategoriaFinanceiraService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaFinanceiraResponse criar(@Valid @RequestBody CategoriaFinanceiraRequest request) {
        return categoriaService.criar(request);
    }

    @GetMapping
    public List<CategoriaFinanceiraResponse> listar(
            @RequestParam(required = false) TipoCategoriaFinanceira tipo) {
        return categoriaService.listar(tipo);
    }
}