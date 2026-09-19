package com.fablab.auth;

import com.fablab.auth.config.JwtProperties;
import com.fablab.auth.config.SeedProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Auth &amp; Identity Service do Sistema de Gestão FabLab.
 *
 * <p>Responsável pela autenticação (usuário/senha), emissão e validação de
 * tokens JWT (chave guardada compartilhada com os demais serviços), RBAC por
 * nível e responsabilidades por módulo/equipamento. No MVP o perfil é
 * cadastrado diretamente no banco de dados do Auth Service, espelhando os
 * níveis do Pessoas &amp; RH (0-Admin, 1-Bolsista, 2-Voluntário, 3-Estagiário,
 * 4-Recrutando).</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties({JwtProperties.class, SeedProperties.class})
public class AuthApplication {

    /** Ponto de entrada da aplicação. */
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}