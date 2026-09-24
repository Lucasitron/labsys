package com.fablab.dashboard.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fablab.dashboard.controller.TaskController;
import com.fablab.dashboard.dto.AuthPrincipal;
import com.fablab.dashboard.dto.CompleteTaskRequest;
import com.fablab.dashboard.service.CompletedTasks;
import com.fablab.dashboard.service.DominioClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Vínculo de conclusão de tarefa ao usuário autenticado.
 */
class TaskControllerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private DominioClient dominioClient;
    private TaskController controller;

    @BeforeEach
    void setUp() {
        dominioClient = mock(DominioClient.class);
        controller = new TaskController(new CompletedTasks(), dominioClient);
    }

    private MockHttpServletRequest requestComBearer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-do-usuario");
        return request;
    }

    @Test
    void adminConcluiSemConsultarVinculo() {
        ResponseEntity<Map<String, Object>> response = controller.complete("rh-cert-42",
                new CompleteTaskRequest(true),
                new AuthPrincipal(1L, "admin", "ADMIN", null), requestComBearer());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void donoConcluiPropriaTarefa() throws Exception {
        when(dominioClient.solicitacoes(null, "token-do-usuario")).thenReturn(List.of(
                mapper.readTree("{\"idSolicitacao\": 42, \"status\": \"PENDENTE\"}")));

        ResponseEntity<Map<String, Object>> response = controller.complete("rh-cert-42",
                new CompleteTaskRequest(true),
                new AuthPrincipal(7L, "joao", "VOLUNTARIO", "marketing"), requestComBearer());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void naoDonoRecebe403() throws Exception {
        when(dominioClient.solicitacoes(null, "token-do-usuario")).thenReturn(List.of(
                mapper.readTree("{\"idSolicitacao\": 99, \"status\": \"PENDENTE\"}")));

        ResponseEntity<Map<String, Object>> response = controller.complete("rh-cert-42",
                new CompleteTaskRequest(true),
                new AuthPrincipal(7L, "joao", "VOLUNTARIO", "marketing"), requestComBearer());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void idDesconhecidoRecebe403ParaNaoAdmin() {
        when(dominioClient.solicitacoes(null, "token-do-usuario")).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.complete("tarefa-qualquer",
                new CompleteTaskRequest(true),
                new AuthPrincipal(7L, "joao", "VOLUNTARIO", "marketing"), requestComBearer());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
