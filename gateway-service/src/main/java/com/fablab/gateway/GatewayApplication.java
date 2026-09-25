package com.fablab.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway do Sistema de Gestão FabLab (Spring Cloud Gateway).
 *
 * <p>Porta de entrada única da plataforma: roteamento via descoberta de
 * serviços (Eureka), balanceamento de carga e ponto central para as políticas
 * de autenticação/autorização.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    /** Ponto de entrada da aplicação. */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}