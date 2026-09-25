package com.fablab.producao.controller;

import com.fablab.producao.dto.AuditoriaProjetoMesaResponse;
import com.fablab.producao.dto.ProjetoMesaRequest;
import com.fablab.producao.dto.ProjetoMesaResponse;
import com.fablab.producao.entity.StatusProjetoMesa;
import com.fablab.producao.service.ProjetoMesaService;
import com.fablab.producao.service.QrCodeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

/** Endpoints de projetos de mesa e seus QR Codes. */
@RestController
@RequestMapping("/projetos-mesa")
public class ProjetoMesaController {

    private final ProjetoMesaService projetoMesaService;
    private final QrCodeService qrCodeService;

    public ProjetoMesaController(ProjetoMesaService projetoMesaService, QrCodeService qrCodeService) {
        this.projetoMesaService = projetoMesaService;
        this.qrCodeService = qrCodeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<ProjetoMesaResponse> criar(@Valid @RequestBody ProjetoMesaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetoMesaService.criar(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public List<ProjetoMesaResponse> listar(@RequestParam(required = false) StatusProjetoMesa status) {
        return projetoMesaService.listar(status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public ProjetoMesaResponse buscar(@PathVariable Long id) {
        return projetoMesaService.buscar(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ProjetoMesaResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProjetoMesaRequest request) {
        return projetoMesaService.atualizar(id, request);
    }

    @PutMapping("/{id}/evolucao")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ProjetoMesaResponse registrarEvolucao(@PathVariable Long id) {
        return projetoMesaService.registrarEvolucao(id);
    }

    @GetMapping(value = "/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public ResponseEntity<byte[]> qrCode(@PathVariable Long id) {
        byte[] png = qrCodeService.gerarPng(projetoMesaService.conteudoQrCode(id));
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    @GetMapping("/{id}/auditorias")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO','ESTAGIARIO')")
    public List<AuditoriaProjetoMesaResponse> auditorias(@PathVariable Long id) {
        return projetoMesaService.listarAuditorias(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BOLSISTA','VOLUNTARIO')")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        projetoMesaService.remover(id);
        return ResponseEntity.noContent().build();
    }
}