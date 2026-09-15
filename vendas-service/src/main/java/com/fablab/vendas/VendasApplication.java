package com.fablab.vendas;

import com.fablab.vendas.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Vendas &amp; CRM Service do Sistema de Gestão FabLab.
 *
 * <p>Responsável pela gestão comercial: clientes (PF/PJ), orçamentos, ciclo de
 * vida das encomendas com Kanban, emissão de recibo interno, registro de vendas
 * em marketplaces e rotinas internas de CRM/marketing.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(JwtProperties.class)
public class VendasApplication {

    /** Ponto de entrada da aplicação. */
    public static void main(String[] args) {
        SpringApplication.run(VendasApplication.class, args);
    }
}