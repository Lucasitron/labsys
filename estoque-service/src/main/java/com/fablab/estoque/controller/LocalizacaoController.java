package com.fablab.estoque.controller;

import com.fablab.estoque.dto.LocalizacaoRequest;
import com.fablab.estoque.dto.LocalizacaoResponse;
import com.fablab.estoque.service.LocalizacaoService;
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
 * Endpoints de localizações físicas.
 */
@RestController
@RequestMapping("/localizacoes")
public class LocalizacaoController {

    private static final String VISUALIZACAO =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";

    private static final String EDICAO =
            "hasAnyRole('ADMIN','BOLSISTA')";

    private final LocalizacaoService localizacaoService;

    public LocalizacaoController(LocalizacaoService localizacaoService) {
        this.localizacaoService = localizacaoService;
    }

    @GetMapping
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<List<LocalizacaoResponse>> listar() {
        return ResponseEntity.ok(localizacaoService.listar());
    }

    /** Cria uma nova localização (tela {@code /localizacoes}). */
    @PostMapping
    @PreAuthorize(EDICAO)
    public ResponseEntity<LocalizacaoResponse> criar(@Valid @RequestBody LocalizacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(localizacaoService.criar(request));
    }
}