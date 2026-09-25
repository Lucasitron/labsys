package com.fablab.vendas.controller;

import com.fablab.vendas.dto.ClienteDtos.BulkTagRequest;
import com.fablab.vendas.dto.ClienteDtos.ClienteDetalheResponse;
import com.fablab.vendas.dto.ClienteDtos.ClientePaginaResponse;
import com.fablab.vendas.dto.ClienteDtos.ClienteRequest;
import com.fablab.vendas.dto.ClienteDtos.ClienteResponse;
import com.fablab.vendas.dto.ClienteDtos.VinculoTagRequest;
import com.fablab.vendas.dto.VendasPrincipal;
import com.fablab.vendas.service.ClienteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de clientes (filtros, detalhe com KPIs, tags, bulk-tag). */
@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private static final String LEITURA = "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";
    private static final String ESCRITA = "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')";

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    @PreAuthorize(ESCRITA)
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request,
                                                 @AuthenticationPrincipal VendasPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.criar(request, principal));
    }

    @GetMapping
    @PreAuthorize(LEITURA)
    public ResponseEntity<ClientePaginaResponse> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) List<Long> tags,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(clienteService.listar(search, tipo, tags, page, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize(LEITURA)
    public ResponseEntity<ClienteDetalheResponse> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.detalhar(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ESCRITA)
    public ResponseEntity<ClienteResponse> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody ClienteRequest request,
                                                     @AuthenticationPrincipal VendasPrincipal principal) {
        return ResponseEntity.ok(clienteService.atualizar(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ESCRITA)
    public ResponseEntity<Void> excluir(@PathVariable Long id,
                                        @AuthenticationPrincipal VendasPrincipal principal) {
        clienteService.excluir(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/tags")
    @PreAuthorize(ESCRITA)
    public ResponseEntity<Void> vincularTag(@PathVariable Long id,
                                            @Valid @RequestBody VinculoTagRequest request,
                                            @AuthenticationPrincipal VendasPrincipal principal) {
        clienteService.vincularTag(id, request.tagId(), principal);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    @PreAuthorize(ESCRITA)
    public ResponseEntity<Void> desvincularTag(@PathVariable Long id,
                                               @PathVariable Long tagId,
                                               @AuthenticationPrincipal VendasPrincipal principal) {
        clienteService.desvincularTag(id, tagId, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-tag")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> bulkTag(@Valid @RequestBody BulkTagRequest request,
                                                       @AuthenticationPrincipal VendasPrincipal principal) {
        return ResponseEntity.ok(clienteService.bulkTag(request, principal));
    }
}
