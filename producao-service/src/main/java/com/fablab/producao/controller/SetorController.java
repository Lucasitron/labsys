package com.fablab.producao.controller;

import com.fablab.producao.dto.ChecklistItemRequest;
import com.fablab.producao.dto.ChecklistItemResponse;
import com.fablab.producao.dto.MaterialSetorRequest;
import com.fablab.producao.dto.MaterialSetorResponse;
import com.fablab.producao.dto.ResponsavelSetorRequest;
import com.fablab.producao.dto.ResponsavelSetorResponse;
import com.fablab.producao.dto.SetorRequest;
import com.fablab.producao.dto.SetorResponse;
import com.fablab.producao.dto.SinalizacaoSetorRequest;
import com.fablab.producao.dto.SinalizacaoSetorResponse;
import com.fablab.producao.service.SetorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de setores 5S, materiais, sinalizações, checklists e responsáveis. */
@RestController
@RequestMapping("/setores")
public class SetorController {

    private final SetorService setorService;

    public SetorController(SetorService setorService) {
        this.setorService = setorService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<SetorResponse> criar(@Valid @RequestBody SetorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(setorService.criar(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public List<SetorResponse> listar(@RequestParam(required = false) Boolean ativo) {
        return setorService.listar(ativo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public SetorResponse buscar(@PathVariable Long id) {
        return setorService.buscar(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public SetorResponse atualizar(@PathVariable Long id, @Valid @RequestBody SetorRequest request) {
        return setorService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        setorService.remover(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/materiais")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<MaterialSetorResponse> adicionarMaterial(@PathVariable Long id,
                                                                   @Valid @RequestBody MaterialSetorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(setorService.adicionarMaterial(id, request));
    }

    @DeleteMapping("/{id}/materiais/{idMaterial}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<Void> removerMaterial(@PathVariable Long id, @PathVariable Long idMaterial) {
        setorService.removerMaterial(id, idMaterial);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sinalizacoes")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<SinalizacaoSetorResponse> adicionarSinalizacao(
            @PathVariable Long id, @Valid @RequestBody SinalizacaoSetorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(setorService.adicionarSinalizacao(id, request));
    }

    @DeleteMapping("/{id}/sinalizacoes/{idSinalizacao}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<Void> removerSinalizacao(@PathVariable Long id, @PathVariable Long idSinalizacao) {
        setorService.removerSinalizacao(id, idSinalizacao);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/checklist")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<ChecklistItemResponse> adicionarChecklist(@PathVariable Long id,
                                                                    @Valid @RequestBody ChecklistItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(setorService.adicionarChecklist(id, request));
    }

    @PutMapping("/{id}/checklist/{idItem}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ChecklistItemResponse atualizarChecklist(@PathVariable Long id,
                                                    @PathVariable Long idItem,
                                                    @Valid @RequestBody ChecklistItemRequest request) {
        return setorService.atualizarChecklist(id, idItem, request);
    }

    @DeleteMapping("/{id}/checklist/{idItem}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<Void> removerChecklist(@PathVariable Long id, @PathVariable Long idItem) {
        setorService.removerChecklist(id, idItem);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/responsaveis")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<ResponsavelSetorResponse> adicionarResponsavel(
            @PathVariable Long id, @Valid @RequestBody ResponsavelSetorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(setorService.adicionarResponsavel(id, request));
    }

    @GetMapping("/{id}/responsaveis")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public List<ResponsavelSetorResponse> listarResponsaveis(@PathVariable Long id) {
        return setorService.buscar(id).responsaveis();
    }

    @DeleteMapping("/{id}/responsaveis/{idResponsavel}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<Void> removerResponsavel(@PathVariable Long id, @PathVariable Long idResponsavel) {
        setorService.removerResponsavel(id, idResponsavel);
        return ResponseEntity.noContent().build();
    }
}