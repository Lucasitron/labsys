package com.fablab.dashboard;

import com.fablab.dashboard.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Dashboard Agregador do Sistema de Gestão FabLab.
 *
 * <p>Serve {@code GET /dashboard/summary} e {@code PATCH /tasks/{id}} do App
 * Shell, agregando dados buscados dos serviços de domínio (no MVP,
 * primordialmente do Pessoas &amp; RH) com o token do usuário reenviado.
 * Tolerante a falhas: quando um serviço consultado está indisponível, o
 * respectivo bloco é degradado (vazio/zero).</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(JwtProperties.class)
public class DashboardApplication {

    /** Ponto de entrada da aplicação. */
    public static void main(String[] args) {
        SpringApplication.run(DashboardApplication.class, args);
    }
}