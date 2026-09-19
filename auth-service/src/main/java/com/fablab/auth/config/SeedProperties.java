package com.fablab.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Usuário raiz criado automaticamente quando o banco está vazio (MVP).
 *
 * @param username usuário de acesso
 * @param password senha em texto puro (entrada do ambiente)
 * @param name     nome completo exibido no perfil
 * @param email    e-mail do usuário
 */
@ConfigurationProperties(prefix = "auth.seed")
public record SeedProperties(String username, String password, String name, String email) {
}