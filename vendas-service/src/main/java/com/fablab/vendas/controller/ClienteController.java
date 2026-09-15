package com.fablab.vendas.controller;

import com.fablab.vendas.dto.ClienteRequest;
import com.fablab.vendas.dto.ClienteResponse;
import com.fablab.vendas.entity.TipoPessoa;
import com.fablab.vendas.service.ClienteService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de cadastro e consulta de clientes.
 */
@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private static final String VISUALIZACAO =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";

    private static final String ESCRITA =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')";

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    @PreAuthorize(ESCRITA)
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.criar(request));
    }

    @GetMapping
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<List<ClienteResponse>> listar(
            @RequestParam(required = false) TipoPessoa tipo,
            @RequestParam(required = false) Long idTag,
            @RequestParam(required = false) String nome) {
        return ResponseEntity.ok(clienteService.listar(tipo, idTag, nome));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<ClienteResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscar(id));
    }

    @PostMapping("/{id}/tags/{idTag}")
    @PreAuthorize(ESCRITA)
    public ResponseEntity<ClienteResponse> adicionarTag(@PathVariable Long id,
                                                        @PathVariable Long idTag) {
        return ResponseEntity.ok(clienteService.adicionarTag(id, idTag));
    }
}