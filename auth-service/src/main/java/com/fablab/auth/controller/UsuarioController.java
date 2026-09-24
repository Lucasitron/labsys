package com.fablab.auth.controller;

import com.fablab.auth.dto.Usuarios;
import com.fablab.auth.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestão de contas de acesso (Admin-only, server-side — C-1).
 *
 * <p>Opera conta/nível/situação sobre {@code login} +
 * {@code user_permissions}. Identidade física é do {@code rh-service}.</p>
 */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<Usuarios.PaginaUsuariosResponse> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, name = "nivel") List<Integer> niveis,
            @RequestParam(required = false, name = "situacao") List<String> situacoes,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(usuarioService.listar(search, niveis, situacoes, page, size));
    }

    @PostMapping
    public ResponseEntity<Usuarios.UsuarioResponse> criar(
            @Valid @RequestBody Usuarios.CriarUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuarios.UsuarioResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody Usuarios.AtualizarUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Usuarios.UsuarioResponse> alterarStatus(
            @PathVariable Long id,
            @Valid @RequestBody Usuarios.AlterarStatusRequest request) {
        return ResponseEntity.ok(usuarioService.alterarStatus(id, request.situacao()));
    }
}
