package com.fablab.estoque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Estoque &amp; Suprimentos Service do Sistema de Gestão FabLab.
 *
 * <p>Responsável pelo inventário (insumos, ferramentas, peças), entradas, saídas,
 * empréstimos, fornecedores, localização física dos itens e Lista de Materiais
 * (BOM) com baixa automática durante a produção.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class EstoqueApplication {

    /** Ponto de entrada da aplicação. */
    public static void main(String[] args) {
        SpringApplication.run(EstoqueApplication.class, args);
    }
}