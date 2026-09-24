package com.fablab.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuração geral da aplicação.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(JwtProperties.class)
public class AppConfig {
}