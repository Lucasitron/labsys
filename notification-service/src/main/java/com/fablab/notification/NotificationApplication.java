package com.fablab.notification;

import com.fablab.notification.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Notification Service do Sistema de Gestão FabLab.
 *
 * <p>Serviço transversal responsável por consumir eventos do RabbitMQ e disparar
 * notificações (e-mail no MVP), manter o histórico com retenção de 90 dias e
 * permitir a revisão pelo Admin. No MVP desta branch, serve a contagem de não
 * lidas do App Shell ({@code GET /notifications/unread/count}); o consumo dos
 * eventos de certificado/extrato vive na feature
 * {@code feature/notification-certificado}.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(JwtProperties.class)
public class NotificationApplication {

    /** Ponto de entrada da aplicação. */
    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}