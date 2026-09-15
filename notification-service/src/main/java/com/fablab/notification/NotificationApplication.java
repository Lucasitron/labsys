package com.fablab.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Notification Service do Sistema de Gestão FabLab.
 *
 * <p>Serviço transversal responsável por consumir eventos do RabbitMQ e disparar
 * notificações (e-mail no MVP), manter o histórico com retenção de 90 dias e
 * permitir a revisão pelo Admin.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class NotificationApplication {

    /** Ponto de entrada da aplicação. */
    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}