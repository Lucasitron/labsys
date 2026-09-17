package com.fablab.notification.controller;

import com.fablab.notification.dto.ConfiguracaoCanalRequest;
import com.fablab.notification.dto.ConfiguracaoCanalResponse;
import com.fablab.notification.service.ConfiguracaoCanalService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de configuração de canais de notificação (Admin). */
@RestController
@RequestMapping("/configuracoes-canal")
@PreAuthorize("hasRole('ADMIN')")
public class ConfiguracaoCanalController {

    private final ConfiguracaoCanalService configuracaoCanalService;

    public ConfiguracaoCanalController(ConfiguracaoCanalService configuracaoCanalService) {
        this.configuracaoCanalService = configuracaoCanalService;
    }

    @GetMapping
    public List<ConfiguracaoCanalResponse> listar() {
        return configuracaoCanalService.listar();
    }

    @GetMapping("/{id}")
    public ConfiguracaoCanalResponse buscar(@PathVariable Long id) {
        return configuracaoCanalService.buscar(id);
    }

    @PutMapping("/{id}")
    public ConfiguracaoCanalResponse atualizar(@PathVariable Long id,
                                               @Valid @RequestBody ConfiguracaoCanalRequest request) {
        return configuracaoCanalService.atualizar(id, request);
    }
}
