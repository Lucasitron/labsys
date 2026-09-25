package com.fablab.estoque.controller;

import com.fablab.estoque.dto.ItemRequest;
import com.fablab.estoque.dto.ItemResponse;
import com.fablab.estoque.entity.Categoria;
import com.fablab.estoque.service.ItemService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints de itens de inventário, incluindo importação/exportação CSV.
 */
@RestController
@RequestMapping("/itens")
public class ItemController {

    private static final String VISUALIZACAO =
            "hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')";

    private static final String EDICAO =
            "hasAnyRole('ADMIN','BOLSISTA')";

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    @PreAuthorize(EDICAO)
    public ResponseEntity<ItemResponse> criar(@Valid @RequestBody ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.criar(request));
    }

    @GetMapping
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<List<ItemResponse>> listar(
            @RequestParam(required = false) Categoria categoria,
            @RequestParam(required = false) Long idLocalizacao,
            @RequestParam(required = false) Boolean baixo) {
        return ResponseEntity.ok(itemService.listar(categoria, idLocalizacao, baixo));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<ItemResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.buscar(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(EDICAO)
    public ResponseEntity<ItemResponse> atualizar(@PathVariable Long id,
                                                  @Valid @RequestBody ItemRequest request) {
        return ResponseEntity.ok(itemService.atualizar(id, request));
    }

    /** Exportação CSV de (download) — visível para todos os níveis operacionais. */
    @GetMapping("/export")
    @PreAuthorize(VISUALIZACAO)
    public ResponseEntity<String> exportarCsv() {
        String csv = itemService.exportarCsv();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"itens.csv\"")
                .body(csv);
    }

    /** Importação CSV (upload) — criação em lote de itens (edição). */
    @PostMapping("/import")
    @PreAuthorize(EDICAO)
    public ResponseEntity<List<ItemResponse>> importarCsv(@RequestPart("arquivo") MultipartFile arquivo)
            throws IOException {
        String conteudo = new String(arquivo.getBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.importarCsv(conteudo));
    }
}