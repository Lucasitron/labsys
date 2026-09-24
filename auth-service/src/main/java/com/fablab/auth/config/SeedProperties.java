package com.fablab.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do seed de acesso admin (dev-only, nunca produção).
 *
 * <p>Prefixo {@code auth.seed}; valores com defaults via env {@code AUTH_ROOT_*}
 * (precedente §8.1) definidos em {@code application.yml}. O seed só executa com
 * {@code auth.seed.enabled=true} (ligado em {@code application-dev.yml}).</p>
 *
 * @param enabled     liga/desliga o seed (default false — prod nunca semeia)
 * @param email       e-mail do login raiz
 * @param nomeUsuario nome de usuário do login raiz
 * @param senha       senha em texto puro (entrada do ambiente, nunca logada)
 * @param idUser      referência ao usuário no RH
 * @param uuid        cartão RFID raiz
 * @param setor       setor do login raiz
 */
@ConfigurationProperties(prefix = "auth.seed")
public record SeedProperties(
        boolean enabled,
        String email,
        String nomeUsuario,
        String senha,
        Long idUser,
        String uuid,
        String setor) {
}
