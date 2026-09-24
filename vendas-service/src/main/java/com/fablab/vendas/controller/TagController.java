package com.fablab.vendas.controller;

import com.fablab.vendas.dto.TagDtos.TagListaResponse;
import com.fablab.vendas.dto.TagDtos.TagRequest;
import com.fablab.vendas.dto.TagDtos.TagResponse;
import com.fablab.vendas.service.TagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de tags de clientes. */
@RestController
@RequestMapping("/tags")
public class TagController {

    private static final String LEITURA = "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";
    private static final String ESCRITA = "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')";

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping
    @PreAuthorize(ESCRITA)
    public ResponseEntity<TagResponse> criar(@Valid @RequestBody TagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.criar(request));
    }

    @GetMapping
    @PreAuthorize(LEITURA)
    public ResponseEntity<TagListaResponse> listar() {
        return ResponseEntity.ok(tagService.listar());
    }
}
