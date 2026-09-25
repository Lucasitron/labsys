package com.fablab.rh.controller;

import com.fablab.rh.dto.ExclusaoResponse;
import com.fablab.rh.dto.PessoaDetalheResponse;
import com.fablab.rh.dto.PessoaRequest;
import com.fablab.rh.dto.PessoaResponse;
import com.fablab.rh.dto.PessoasPaginaResponse;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.PessoaStatus;
import com.fablab.rh.service.PessoaService;
import jakarta.validation.Valid;
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

/**
 * Endpoints de gestão de pessoas.
 */
@RestController
@RequestMapping("/pessoas")
public class PessoaController {

    private final PessoaService pessoaService;

    public PessoaController(PessoaService pessoaService) {
        this.pessoaService = pessoaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO')")
    public ResponseEntity<PessoaResponse> cadastrar(@Valid @RequestBody PessoaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pessoaService.cadastrar(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO', 'ESTAGIARIO')")
    public ResponseEntity<PessoasPaginaResponse> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, name = "setor") String departamento,
            @RequestParam(required = false) NivelAcesso nivel,
            @RequestParam(required = false) PessoaStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(
                pessoaService.listar(search, departamento, nivel, status, page, pageSize, principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PessoaResponse> buscar(@PathVariable Long id,
                                                 @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(pessoaService.buscar(id, principal));
    }

    @GetMapping("/{id}/detalhe")
    public ResponseEntity<PessoaDetalheResponse> detalhe(@PathVariable Long id,
                                                         @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(pessoaService.detalhe(id, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExclusaoResponse> excluir(@PathVariable Long id) {
        pessoaService.excluir(id);
        return ResponseEntity.ok(new ExclusaoResponse(true, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BOLSISTA', 'VOLUNTARIO')")
    public ResponseEntity<PessoaResponse> atualizar(@PathVariable Long id,
                                                    @Valid @RequestBody PessoaRequest request,
                                                    @AuthenticationPrincipal RhPrincipal principal) {
        return ResponseEntity.ok(pessoaService.atualizar(id, request, principal));
    }
}