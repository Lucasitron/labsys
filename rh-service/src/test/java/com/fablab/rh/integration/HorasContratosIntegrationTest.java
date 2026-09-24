package com.fablab.rh.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.RegistroPontoDiario;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Contratos de horas (D-1/D-5): recálculo início/fim, lista com filtros,
 * fachada {@code /horas} e motivo de rejeição.
 */
class HorasContratosIntegrationTest extends BaseIntegrationTest {

    @Test
    void registraRecalculandoPeloHorario() throws Exception {
        Pessoa ana = seedPessoa("Ana Souza", "MAT-070");
        Funcionario func = seedFuncionario(ana, NivelAcesso.BOLSISTA, "Mecatrônica");
        String token = token(ana.getId(), NivelAcesso.BOLSISTA, func.getId());

        mockMvc.perform(post("/apontamentos-horas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idFuncionario":%d,"tipo":"ENCOMENDA","idReferencia":1044,
                                 "data":"%s","horasTrabalhadas":9.99,
                                 "horaInicio":"14:00","horaFim":"17:00",
                                 "descricaoAtividade":"Pedido"}
                                """.formatted(func.getId(), LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horasTrabalhadas").value(3.0))
                .andExpect(jsonPath("$.horaInicio").value("14:00:00"))
                .andExpect(jsonPath("$.horaFim").value("17:00:00"));
    }

    @Test
    void registraRejeitaHorarioInconsistente() throws Exception {
        Pessoa ana = seedPessoa("Ana Souza", "MAT-071");
        Funcionario func = seedFuncionario(ana, NivelAcesso.BOLSISTA, "Mecatrônica");
        String token = token(ana.getId(), NivelAcesso.BOLSISTA, func.getId());

        mockMvc.perform(post("/apontamentos-horas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idFuncionario":%d,"tipo":"PROJETO","idReferencia":3,
                                 "data":"%s","horaInicio":"17:00","horaFim":"14:00"}
                                """.formatted(func.getId(), LocalDate.now())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/apontamentos-horas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idFuncionario":%d,"tipo":"PROJETO","idReferencia":3,
                                 "data":"%s"}
                                """.formatted(func.getId(), LocalDate.now())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaComFiltrosEEscopoProprio() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM-HOR");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        Pessoa ana = seedPessoa("Ana Souza", "MAT-072");
        Funcionario funcAna = seedFuncionario(ana, NivelAcesso.VOLUNTARIO, "Mecatrônica");
        String anaToken = token(ana.getId(), NivelAcesso.VOLUNTARIO, funcAna.getId());

        registrar(adminToken, admin.getId(), LocalDate.now());
        registrar(anaToken, funcAna.getId(), LocalDate.now());

        String periodo = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        mockMvc.perform(get("/apontamentos-horas")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDENTE")
                        .param("periodo", periodo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/horas")
                        .header("Authorization", "Bearer " + anaToken)
                        .param("periodo", periodo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/horas")
                        .header("Authorization", "Bearer " + anaToken)
                        .param("status", "VALIDADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void fachadaValidaERejeitaComMotivo() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM-HOR2");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        RegistroPontoDiario registro = new RegistroPontoDiario();
        registro.setFuncionario(admin);
        registro.setData(LocalDate.now());
        registro.setHoraEntrada(Instant.now().minusSeconds(8 * 3600));
        registro.setHoraSaida(Instant.now());
        registro.setTotalHoras(new BigDecimal("8.00"));
        pontoRepository.save(registro);

        long idValidar = extrairId(registrar(adminToken, admin.getId(), LocalDate.now()));
        mockMvc.perform(patch("/horas/{id}/validar", idValidar)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDADO"));

        long idRejeitar = extrairId(registrar(adminToken, admin.getId(), LocalDate.now()));
        mockMvc.perform(patch("/horas/{id}/rejeitar", idRejeitar)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"Fora do escopo do projeto"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADO"))
                .andExpect(jsonPath("$.motivoRejeicao").value("Fora do escopo do projeto"));
    }

    @Test
    void semVinculoNaoListaHoras() throws Exception {
        Pessoa semVinculo = seedPessoa("Sem Vinculo", "MAT-073");
        String tokenSemVinculo = token(semVinculo, NivelAcesso.BOLSISTA);

        mockMvc.perform(get("/horas")
                        .header("Authorization", "Bearer " + tokenSemVinculo))
                .andExpect(status().isForbidden());
    }

    private String registrar(String token, long idFuncionario, LocalDate data) throws Exception {
        return mockMvc.perform(post("/horas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idFuncionario":%d,"tipo":"PROJETO","idReferencia":9,
                                 "data":"%s","horasTrabalhadas":2.00}
                                """.formatted(idFuncionario, data)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private long extrairId(String json) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("id").asLong();
    }
}
