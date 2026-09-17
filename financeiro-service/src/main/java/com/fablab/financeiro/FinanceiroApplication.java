package com.fablab.financeiro;

import com.fablab.financeiro.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Financeiro Service do Sistema de Gestão FabLab.
 *
 * <p>Responsável pelo fluxo de caixa, contas a pagar/receber, doações e recursos de
 * projetos, solicitações de compra (fluxo informativo), custeio por ordem de produção
 * (Job Order Costing) e relatórios de saúde financeira. Acesso exclusivo do Admin.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(JwtProperties.class)
public class FinanceiroApplication {

    /** Ponto de entrada da aplicação. */
    public static void main(String[] args) {
        SpringApplication.run(FinanceiroApplication.class, args);
    }
}