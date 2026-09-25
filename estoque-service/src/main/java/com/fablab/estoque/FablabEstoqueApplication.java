package com.fablab.estoque;

import com.fablab.estoque.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Estoque &amp; Suprimentos Service do Sistema de Gestão FabLab.
 *
 * <p>Responsável pelo inventário (insumos, ferramentas, peças), entradas, saídas,
 * empréstimos, fornecedores, localização física dos itens e Lista de Materiais
 * (BOM) com baixa automática durante a produção.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableConfigurationProperties(JwtProperties.class)
public class FablabEstoqueApplication {

    /** Ponto de entrada da aplicação. */
    public static void main(String[] args) {
        SpringApplication.run(FablabEstoqueApplication.class, args);
    }
}