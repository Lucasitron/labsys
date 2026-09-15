package com.fablab.producao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Produção &amp; Projetos Service do Sistema de Gestão FabLab.
 *
 * <p>Responsável pela gestão operacional: projetos e tarefas, Kanban de
 * encomendas com baixa automática de estoque (BOM), máquinas e histórico de uso,
 * e o sistema de gestão 5S (setores, inspeções, advertências e penalidades).</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ProducaoApplication {

    /** Ponto de entrada da aplicação. */
    public static void main(String[] args) {
        SpringApplication.run(ProducaoApplication.class, args);
    }
}