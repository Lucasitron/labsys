package com.fablab.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente tolerante a falhas dos serviços de domínio, resolvido via Eureka.
 *
 * <p>Cada chamada reenvia o token do usuário autenticado. Quando o serviço
 * consultado está indisponível, devolve coleção vazia/zero para que o resumo
 * degrade com elegância.</p>
 */
@Component
public class DominioClient {

    private static final Logger log = LoggerFactory.getLogger(DominioClient.class);

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public DominioClient(DiscoveryClient discoveryClient, ObjectMapper objectMapper) {
        this.discoveryClient = discoveryClient;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** Lista de solicitações de certificado (opcionalmente filtrada por status). */
    public List<JsonNode> solicitacoes(String status, String bearer) {
        String url = resolve("rh-service");
        if (url == null) {
            return List.of();
        }
        String uri = "/certificados/solicitacoes" + (status == null ? "" : "?status=" + status);
        return arrayOrEmpty(get(url + uri, bearer));
    }

    /** Lista de certificados emitidos. */
    public List<JsonNode> emitidos(String bearer) {
        String url = resolve("rh-service");
        if (url == null) {
            return List.of();
        }
        return arrayOrEmpty(get(url + "/certificados/emitidos", bearer));
    }

    /** Quantidade de notificações não lidas do usuário (0 se o serviço estiver fora). */
    public int unreadCount(String bearer) {
        String url = resolve("notification-service");
        if (url == null) {
            return 0;
        }
        JsonNode body = get(url + "/notifications/unread/count", bearer);
        if (body == null || !body.has("count")) {
            return 0;
        }
        return body.get("count").asInt(0);
    }

    private JsonNode get(String url, String bearer) {
        try {
            return restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception ex) {
            log.debug("Falha ao consultar {}: {}", url, ex.getMessage());
            return null;
        }
    }

    private List<JsonNode> arrayOrEmpty(JsonNode node) {
        java.util.List<JsonNode> items = new java.util.ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(items::add);
        }
        return items;
    }

    private String resolve(String serviceId) {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            if (instances.isEmpty()) {
                log.debug("Instância de '{}' não encontrada no Eureka", serviceId);
                return null;
            }
            ServiceInstance instance = instances.get(0);
            return "http://" + instance.getHost() + ":" + instance.getPort();
        } catch (Exception ex) {
            log.debug("Não foi possível resolver '{}': {}", serviceId, ex.getMessage());
            return null;
        }
    }
}