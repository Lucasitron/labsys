package com.fablab.vendas.controller;

import com.fablab.vendas.dto.MarketplaceDtos.MarketplaceListaResponse;
import com.fablab.vendas.dto.MarketplaceDtos.MarketplaceRequest;
import com.fablab.vendas.dto.MarketplaceDtos.MarketplaceResponse;
import com.fablab.vendas.service.MarketplaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de registro manual de marketplace. */
@RestController
@RequestMapping("/marketplace")
public class MarketplaceController {

    private static final String ACESSO = "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";

    private final MarketplaceService marketplaceService;

    public MarketplaceController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    @PostMapping
    @PreAuthorize(ACESSO)
    public ResponseEntity<MarketplaceResponse> registrar(@Valid @RequestBody MarketplaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marketplaceService.registrar(request));
    }

    @GetMapping
    @PreAuthorize(ACESSO)
    public ResponseEntity<MarketplaceListaResponse> listar(
            @RequestParam(required = false) Long encomendaId,
            @RequestParam(required = false) String plataforma) {
        return ResponseEntity.ok(marketplaceService.listar(encomendaId, plataforma));
    }
}
