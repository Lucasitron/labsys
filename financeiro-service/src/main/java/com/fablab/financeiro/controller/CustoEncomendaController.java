package com.fablab.financeiro.controller;

import com.fablab.financeiro.dto.CustoEncomendaResponse;
import com.fablab.financeiro.service.CustoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Custeio por ordem de produção (Job Order Costing).
 */
@RestController
@RequestMapping("/custos-encomenda")
@PreAuthorize("hasRole('ADMIN')")
public class CustoEncomendaController {

    private final CustoService custoService;

    public CustoEncomendaController(CustoService custoService) {
        this.custoService = custoService;
    }

    @GetMapping("/{idEncomenda}")
    public CustoEncomendaResponse obterPorEncomenda(@PathVariable Long idEncomenda) {
        return custoService.obterPorEncomenda(idEncomenda);
    }
}