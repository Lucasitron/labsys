package com.fablab.notification.service;

import com.fablab.notification.dto.ConfiguracaoCanalRequest;
import com.fablab.notification.dto.ConfiguracaoCanalResponse;
import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.ConfiguracaoCanal;
import com.fablab.notification.exception.ResourceNotFoundException;
import com.fablab.notification.repository.ConfiguracaoCanalRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras de negócio das configurações de canal de notificação. */
@Service
public class ConfiguracaoCanalService {

    private final ConfiguracaoCanalRepository repository;

    public ConfiguracaoCanalService(ConfiguracaoCanalRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ConfiguracaoCanalResponse> listar() {
        return repository.findAll().stream().map(ConfiguracaoCanalResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ConfiguracaoCanalResponse buscar(Long id) {
        return ConfiguracaoCanalResponse.from(obter(id));
    }

    @Transactional
    public ConfiguracaoCanalResponse atualizar(Long id, ConfiguracaoCanalRequest request) {
        ConfiguracaoCanal configuracao = obter(id);
        configuracao.setHabilitado(request.habilitado());
        configuracao.setParametros(request.parametros());
        return ConfiguracaoCanalResponse.from(configuracao);
    }

    /** Indica se o canal está habilitado (falso quando não configurado). */
    @Transactional(readOnly = true)
    public boolean isHabilitado(CanalNotificacao canal) {
        return repository.findByCanal(canal)
                .map(ConfiguracaoCanal::getHabilitado)
                .orElse(false);
    }

    private ConfiguracaoCanal obter(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Configuração de canal", id));
    }
}
