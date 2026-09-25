package com.fablab.estoque.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.estoque.TokenHelper;
import com.fablab.estoque.entity.Categoria;
import com.fablab.estoque.entity.Emprestimo;
import com.fablab.estoque.entity.NivelAcesso;
import com.fablab.estoque.entity.StatusEmprestimo;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class EmprestimoIntegrationTest extends BaseIntegrationTest {

    @Test
    void adminCriaEmprestimo() throws Exception {
        var item = seedItem("Furadeira", Categoria.FERRAMENTA, BigDecimal.TEN, BigDecimal.ONE);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/emprestimos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idItem":%d,"idPessoa":1,"quantidade":3,
                                 "dataDevolucaoPrevista":"2026-12-31"}
                                """.formatted(item.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    void bolsistaCriaEmprestimoParaSiMesmo() throws Exception {
        var item = seedItem("Trena", Categoria.FERRAMENTA, BigDecimal.TEN, BigDecimal.ONE);
        String token = token(5L, NivelAcesso.BOLSISTA);

        mockMvc.perform(post("/emprestimos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idItem":%d,"idPessoa":5,"quantidade":1,
                                 "dataDevolucaoPrevista":"2026-12-31"}
                                """.formatted(item.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void bolsistaNaoPodeEmprestarParaOutro() throws Exception {
        var item = seedItem("Trena", Categoria.FERRAMENTA, BigDecimal.TEN, BigDecimal.ONE);
        String token = token(5L, NivelAcesso.BOLSISTA);

        mockMvc.perform(post("/emprestimos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idItem":%d,"idPessoa":99,"quantidade":1,
                                 "dataDevolucaoPrevista":"2026-12-31"}
                                """.formatted(item.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void devolucaoPorAdmin() throws Exception {
        var item = seedItem("Multímetro", Categoria.FERRAMENTA, BigDecimal.TEN, BigDecimal.ONE);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        String response = mockMvc.perform(post("/emprestimos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idItem":%d,"idPessoa":1,"quantidade":2,
                                 "dataDevolucaoPrevista":"2026-12-31"}
                                """.formatted(item.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long empId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor().newInstance()
                .readTree(response).get("id").asLong();

        mockMvc.perform(put("/emprestimos/{id}/devolucao", empId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEVOLVIDO"));
    }

    @Test
    void devolucaoPorNaoResponsavel403() throws Exception {
        var item = seedItem("Alicate", Categoria.FERRAMENTA, BigDecimal.TEN, BigDecimal.ONE);
        String adminToken = token(1L, NivelAcesso.ADMIN);
        String outroToken = token(2L, NivelAcesso.VOLUNTARIO);

        String response = mockMvc.perform(post("/emprestimos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idItem":%d,"idPessoa":1,"quantidade":1,
                                 "dataDevolucaoPrevista":"2026-12-31"}
                                """.formatted(item.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long empId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor().newInstance()
                .readTree(response).get("id").asLong();

        mockMvc.perform(put("/emprestimos/{id}/devolucao", empId)
                        .header("Authorization", "Bearer " + outroToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarAtrasados() throws Exception {
        var emp = new Emprestimo();
        var item = seedItem("Lupa", Categoria.FERRAMENTA, BigDecimal.TEN, BigDecimal.ONE);
        emp.setItem(item);
        emp.setIdPessoa(1L);
        emp.setQuantidade(BigDecimal.ONE);
        emp.setDataEmprestimo(LocalDate.now().minusDays(10));
        emp.setDataDevolucaoPrevista(LocalDate.now().minusDays(3));
        emp.setStatus(StatusEmprestimo.ATIVO);
        emprestimoRepository.save(emp);

        String adminToken = token(1L, NivelAcesso.ADMIN);
        mockMvc.perform(get("/emprestimos/atrasados").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ATIVO"));
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/emprestimos/atrasados"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void detalheDeEmprestimo() throws Exception {
        var item = seedItem("Furadeira", Categoria.FERRAMENTA, BigDecimal.TEN, BigDecimal.ONE);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        String response = mockMvc.perform(post("/emprestimos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idItem":%d,"idPessoa":1,"quantidade":2,
                                 "dataDevolucaoPrevista":"2026-12-31"}
                                """.formatted(item.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long empId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor().newInstance()
                .readTree(response).get("id").asLong();

        mockMvc.perform(get("/emprestimos/{id}", empId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));
    }
}