package com.fablab.notification.dto;

import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.ConfiguracaoCanal;

/** Representação de uma configuração de canal. */
public record ConfiguracaoCanalResponse(
        Long idConfiguracao,
        CanalNotificacao canal,
        Boolean habilitado,
        String parametros) {

    public static ConfiguracaoCanalResponse from(ConfiguracaoCanal c) {
        return new ConfiguracaoCanalResponse(
                c.getIdConfiguracao(), c.getCanal(), c.getHabilitado(), c.getParametros());
    }
}
