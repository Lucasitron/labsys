package com.fablab.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service Registry do Sistema de Gestão FabLab (Netflix Eureka).
 *
 * <p>Mantém o registro central de todos os microsserviços para descoberta
 * dinâmica de endereços pelo Gateway e demais serviços.</p>
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryApplication {

    /** Ponto de entrada da aplicação. */
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryApplication.class, args);
    }
}