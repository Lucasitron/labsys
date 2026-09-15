package com.fablab.vendas.controller;

import com.fablab.vendas.dto.TagClienteRequest;
import com.fablab.vendas.dto.TagClienteResponse;
import com.fablab.vendas.service.ClienteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de tags de clientes.
 */
@RestController
@RequestMapping("/tags")
public class TagController {

    private static final String VISUALIZACAO =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";

    private static final String ESCRITA =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')";

    private final ClienteService clienteService;

    public TagController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    @PreAuthorize(ESCRITA)
    public ResponseEntity<TagClienteResponse> criar(@Valid @RequestBody TagClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.criarTag(request));
    }

    @GetMapping
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<List<TagClienteResponse>> listar() {
        return ResponseEntity.ok(clienteService.listarTags());
    }
}