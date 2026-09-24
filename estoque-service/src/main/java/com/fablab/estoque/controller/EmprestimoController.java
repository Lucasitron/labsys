package com.fablab.estoque.controller;

import com.fablab.estoque.dto.EmprestimoRequest;
import com.fablab.estoque.dto.EmprestimoResponse;
import com.fablab.estoque.dto.EstoquePrincipal;
import com.fablab.estoque.service.EmprestimoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de empréstimos de equipamentos/ferramentas.
 */
@RestController
@RequestMapping("/emprestimos")
public class EmprestimoController {

    private static final String OPERACAO = "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')";
    private static final String VISUALIZACAO =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";

    private final EmprestimoService emprestimoService;

    public EmprestimoController(EmprestimoService emprestimoService) {
        this.emprestimoService = emprestimoService;
    }

    @PostMapping
    @PreAuthorize(OPERACAO)
    public ResponseEntity<EmprestimoResponse> criar(@Valid @RequestBody EmprestimoRequest request,
                                                    @AuthenticationPrincipal EstoquePrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(emprestimoService.criar(request, principal));
    }

    @PutMapping("/{id}/devolucao")
    @PreAuthorize(OPERACAO)
    public ResponseEntity<EmprestimoResponse> devolver(@PathVariable Long id,
                                                       @AuthenticationPrincipal EstoquePrincipal principal) {
        return ResponseEntity.ok(emprestimoService.devolver(id, principal));
    }

    /**
     * Lista global de empréstimos (E-1/R-9).
     *
     * @param status opcional: {@code ativos}, {@code atrasados} ou {@code historico}
     */
    @GetMapping
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<List<EmprestimoResponse>> listar(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(emprestimoService.listar(status));
    }

    @GetMapping("/atrasados")
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<List<EmprestimoResponse>> atrasados() {
        return ResponseEntity.ok(emprestimoService.listarAtrasados());
    }

    /** Detalhe de um empréstimo (tela {@code /emprestimos/[id]}). */
    @GetMapping("/{id}")
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<EmprestimoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(emprestimoService.buscar(id));
    }
}