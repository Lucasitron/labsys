package com.fablab.estoque.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Lookup fail-soft do nome do tomador no Pessoas &amp; RH (E-4).
 *
 * <p>Consulta {@code GET /pessoas/{id}} (contrato real {@code PessoaResponse}
 * com campo {@code nomeCompleto}) e repassa o Bearer da requisição atual. RH
 * fora/lento/404 ou sem instância no Eureka → {@code Optional.empty()} para
 * que o chamador use o rótulo estável {@code Pessoa #id} (nunca 500 por causa
 * do nome).</p>
 */
@Component
public class RhPessoaClient {

    private static final Logger log = LoggerFactory.getLogger(RhPessoaClient.class);

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final String baseUrlFixa;

    public RhPessoaClient(DiscoveryClient discoveryClient,
                          @Value("${app.rh-service.url:}") String baseUrlFixa) {
        this.discoveryClient = discoveryClient;
        this.baseUrlFixa = baseUrlFixa == null || baseUrlFixa.isBlank() ? null : baseUrlFixa;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(1).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(2).toMillis());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * Busca o nome completo da pessoa no RH.
     *
     * @return nome quando resolvido; vazio em qualquer falha (fail-soft)
     */
    public Optional<String> buscarNome(Long idPessoa) {
        if (idPessoa == null) {
            return Optional.empty();
        }
        String base = baseUrl();
        if (base == null) {
            return Optional.empty();
        }
        try {
            String bearer = bearerAtual();
            JsonNode body = restClient.get()
                    .uri(base + "/pessoas/" + idPessoa)
                    .headers(headers -> {
                        if (bearer != null) {
                            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
                        }
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            if (body != null && body.hasNonNull("nomeCompleto")) {
                String nome = body.get("nomeCompleto").asText("").trim();
                if (!nome.isEmpty()) {
                    return Optional.of(nome);
                }
            }
            return Optional.empty();
        } catch (Exception ex) {
            log.debug("RH indisponível para pessoa {}: {}", idPessoa, ex.getMessage());
            return Optional.empty();
        }
    }

    private String baseUrl() {
        if (baseUrlFixa != null) {
            return baseUrlFixa;
        }
        if (discoveryClient == null) {
            return null;
        }
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances("rh-service");
            if (instances.isEmpty()) {
                log.debug("Instância de 'rh-service' não encontrada no Eureka");
                return null;
            }
            ServiceInstance instance = instances.get(0);
            return "http://" + instance.getHost() + ":" + instance.getPort();
        } catch (Exception ex) {
            log.debug("Não foi possível resolver 'rh-service': {}", ex.getMessage());
            return null;
        }
    }

    private String bearerAtual() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                return header.substring(7);
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }
}
