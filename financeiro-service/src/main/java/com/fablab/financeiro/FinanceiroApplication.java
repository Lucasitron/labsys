package com.fablab.financeiro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Financeiro Service do Sistema de Gestão FabLab.
 *
 * <p>Responsável pelo fluxo de caixa, contas a pagar/receber, doações e recursos,
 * solicitações de compra (informativo), custeio por ordem de produção (Job Order
 * Costing) e relatórios de saúde financeira. Acesso exclusivo do Admin.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class FinanceiroApplication {

    /** Ponto de entrada da aplicação. */
    public static void main(String[] args) {
        SpringApplication.run(FinanceiroApplication.class, args);
    }
}