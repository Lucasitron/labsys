package com.fablab.financeiro;

import com.fablab.financeiro.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Financeiro Service do Sistema de Gestão FabLab.
 *
 * <p>Responsável pelo fluxo de caixa, contas a pagar/receber, doações e recursos,
 * solicitações de compra (informativo), custeio por ordem de produção (Job Order
 * Costing) e relatórios de saúde financeira. Acesso exclusivo do Admin.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableConfigurationProperties(JwtProperties.class)
public class FinanceiroApplication {

    /** Ponto de entrada da aplicação. */
    public static void main(String[] args) {
        SpringApplication.run(FinanceiroApplication.class, args);
    }
}
