package com.fablab.producao.controller;

import com.fablab.producao.dto.Inspecao5SRequest;
import com.fablab.producao.dto.Inspecao5SResponse;
import com.fablab.producao.service.Inspecao5SService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de inspeções 5S. */
@RestController
@RequestMapping("/inspecoes-5s")
public class Inspecao5SController {

    private final Inspecao5SService inspecaoService;

    public Inspecao5SController(Inspecao5SService inspecaoService) {
        this.inspecaoService = inspecaoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO') and @acesso.podeEditar(#request.idInspetor())")
    public ResponseEntity<Inspecao5SResponse> registrar(@Valid @RequestBody Inspecao5SRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inspecaoService.registrar(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public List<Inspecao5SResponse> listar(@RequestParam(required = false) Long idSetor) {
        return inspecaoService.listar(idSetor);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public Inspecao5SResponse buscar(@PathVariable Long id) {
        return inspecaoService.buscar(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        inspecaoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}