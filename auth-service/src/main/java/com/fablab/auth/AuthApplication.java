package com.fablab.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Auth & Identity Service do Sistema de Gestão FabLab.
 *
 * <p>Responsável pela autenticação (e-mail/senha), emissão e validação de tokens
 * JWT, RBAC, controle de sessão (blacklist) e validação de acesso físico via RFID.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AuthApplication {

    /** Ponto de entrada da aplicação. */
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}