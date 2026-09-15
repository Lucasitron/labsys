package com.fablab.rh;

import com.fablab.rh.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Pessoas &amp; RH Service do Sistema de Gestão FabLab.
 *
 * <p>Responsável pela gestão de pessoas (professores, alunos, candidatos),
 * processo seletivo, registro de ponto e horas, treinamento (LMS) e evolução
 * dos níveis de acesso.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(JwtProperties.class)
public class RhApplication {

    /** Ponto de entrada da aplicação. */
    public static void main(String[] args) {
        SpringApplication.run(RhApplication.class, args);
    }
}