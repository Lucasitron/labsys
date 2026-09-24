package com.fablab.rh.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.RegistroPontoDiario;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ApontamentoIntegrationTest extends BaseIntegrationTest {

    @Test
    void bolsistaRegistraPropriasHorasEAdminValida() throws Exception {
        Pessoa pessoa = seedPessoa("Maria", "MAT-200");
        Funcionario funcionario = seedFuncionario(pessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        String bolsistaToken = token(pessoa.getId(), NivelAcesso.BOLSISTA, funcionario.getId());

        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM2");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        RegistroPontoDiario registro = new RegistroPontoDiario();
        registro.setFuncionario(funcionario);
        registro.setData(LocalDate.now());
        registro.setTotalHoras(new BigDecimal("8.00"));
        pontoRepository.save(registro);

        mockMvc.perform(post("/apontamentos-horas")
                        .header("Authorization", "Bearer " + bolsistaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idFuncionario":%d,"tipo":"ENCOMENDA","idReferencia":9,
                                 "data":"%s","horasTrabalhadas":3.00,"descricaoAtividade":"Pedido XYZ"}
                                """.formatted(funcionario.getId(), LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.idFuncionario").value(funcionario.getId()));

        var presentes = apontamentoRepository.findByFuncionarioId(funcionario.getId());
        org.assertj.core.api.Assertions.assertThat(presentes).hasSize(1);
        Long apontamentoId = presentes.get(0).getId();

        mockMvc.perform(put("/apontamentos-horas/{id}/validar", apontamentoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"VALIDADO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDADO"))
                .andExpect(jsonPath("$.idAdminValidador").value(admin.getId()));
    }

    @Test
    void bolsistaNaoPodeRegistrarHorasDeOutro() throws Exception {
        Pessoa pessoa = seedPessoa("Maria", "MAT-201");
        Funcionario funcionario = seedFuncionario(pessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        String bolsistaToken = token(pessoa.getId(), NivelAcesso.BOLSISTA, funcionario.getId());

        Pessoa outro = seedPessoa("João", "MAT-202");
        Funcionario outroFunc = seedFuncionario(outro, NivelAcesso.VOLUNTARIO, "Marcenaria");

        mockMvc.perform(post("/apontamentos-horas")
                        .header("Authorization", "Bearer " + bolsistaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idFuncionario":%d,"tipo":"PROJETO","idReferencia":1,
                                 "data":"%s","horasTrabalhadas":1.00}
                                """.formatted(outroFunc.getId(), LocalDate.now())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRejeitaCoerenciaQuandoHorasExcedemPresenca() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM3");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        RegistroPontoDiario registro = new RegistroPontoDiario();
        registro.setFuncionario(admin);
        registro.setData(LocalDate.now());
        registro.setTotalHoras(new BigDecimal("2.00"));
        pontoRepository.save(registro);

        String response = mockMvc.perform(post("/apontamentos-horas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idFuncionario":%d,"tipo":"PROJETO","idReferencia":4,
                                 "data":"%s","horasTrabalhadas":5.00}
                                """.formatted(admin.getId(), LocalDate.now())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long apontamentoId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("id").asLong();

        mockMvc.perform(put("/apontamentos-horas/{id}/validar", apontamentoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"VALIDADO"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("excedem")));
    }
}