package com.fablab.vendas.controller;

import com.fablab.vendas.dto.MarketplaceRequest;
import com.fablab.vendas.dto.MarketplaceResponse;
import com.fablab.vendas.service.MarketplaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de registro manual de vendas em marketplace.
 */
@RestController
@RequestMapping("/marketplace")
public class MarketplaceController {

    private static final String ESCRITA =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')";

    private final MarketplaceService marketplaceService;

    public MarketplaceController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    @PostMapping
    @PreAuthorize(ESCRITA)
    public ResponseEntity<MarketplaceResponse> registrar(@Valid @RequestBody MarketplaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marketplaceService.registrarVenda(request));
    }
}