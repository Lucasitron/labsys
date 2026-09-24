package com.fablab.auth.controller;

import com.fablab.auth.dto.Permissoes;
import com.fablab.auth.service.RbacService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Matriz RBAC completa + edição de célula (Admin-only, server-side — C-1).
 *
 * <p>Módulo/nível/valor validados contra os enums (C-3: 422 PT se
 * inválido).</p>
 */
@RestController
@RequestMapping("/api/permissoes")
@PreAuthorize("hasRole('ADMIN')")
public class PermissaoController {

    private final RbacService rbacService;

    public PermissaoController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping
    public ResponseEntity<Permissoes.MatrizPermissoesResponse> obter() {
        return ResponseEntity.ok(rbacService.getFullMatrix());
    }

    @PutMapping("/{modulo}/{nivel}")
    public ResponseEntity<Permissoes.CelulaPermissao> atualizar(
            @PathVariable String modulo,
            @PathVariable String nivel,
            @Valid @RequestBody Permissoes.AtualizarPermissaoRequest request) {
        return ResponseEntity.ok(rbacService.updateCell(modulo, nivel, request.valor()));
    }
}
