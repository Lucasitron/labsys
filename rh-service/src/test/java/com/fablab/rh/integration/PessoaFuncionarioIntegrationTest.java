package com.fablab.rh.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.PessoaStatus;
import com.fablab.rh.entity.RegistroPontoDiario;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PessoaFuncionarioIntegrationTest extends BaseIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fluxoDePessoaEFuncionario() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM-FLOW");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        Long pessoaId = readId(mockMvc.perform(post("/pessoas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCompleto":"Maria Silva","matricula":"MAT-001",
                                 "contato":"a@b.com","turno":"Manhã"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeCompleto").value("Maria Silva"))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/pessoas/{id}", pessoaId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricula").value("MAT-001"));

        mockMvc.perform(put("/pessoas/{id}", pessoaId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCompleto":"Maria Silva Santos","matricula":"MAT-001","turno":"Tarde"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeCompleto").value("Maria Silva Santos"))
                .andExpect(jsonPath("$.turno").value("Tarde"));

        Long funcionarioId = readId(mockMvc.perform(post("/funcionarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idPessoa":%d,"nivelAcesso":"BOLSISTA","departamento":"Eletrônica"}
                                """.formatted(pessoaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nivelAcesso").value("BOLSISTA"))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/funcionarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("nivel", "BOLSISTA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(put("/funcionarios/{id}/nivel", funcionarioId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nivelNovo":"ESTAGIARIO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelNovo").value("ESTAGIARIO"));

        assertThat(historicoRepository.findByFuncionarioId(funcionarioId)).hasSize(1);
        verify(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(Object.class));
    }

    @Test
    void horasDoFuncionarioComCoerencia() throws Exception {
        Pessoa pessoa = seedPessoa("Maria", "MAT-100");
        Funcionario funcionario = seedFuncionario(pessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        String bolsistaToken = token(pessoa.getId(), NivelAcesso.BOLSISTA, funcionario.getId());

        RegistroPontoDiario registro = new RegistroPontoDiario();
        registro.setFuncionario(funcionario);
        registro.setData(LocalDate.now());
        registro.setHoraEntrada(Instant.now().minusSeconds(8 * 3600));
        registro.setHoraSaida(Instant.now());
        registro.setTotalHoras(new BigDecimal("8.00"));
        pontoRepository.save(registro);

        mockMvc.perform(get("/funcionarios/{id}/horas", funcionario.getId())
                        .header("Authorization", "Bearer " + bolsistaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHorasPresenca").value(8.0))
                .andExpect(jsonPath("$.incoerencias.length()").value(0));
    }

    @Test
    void apontamentoValidadoPublicaEventoDeHoras() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        RegistroPontoDiario registro = new RegistroPontoDiario();
        registro.setFuncionario(admin);
        registro.setData(LocalDate.now());
        registro.setTotalHoras(new BigDecimal("8.00"));
        pontoRepository.save(registro);

        Long apontamentoId = readId(mockMvc.perform(post("/apontamentos-horas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idFuncionario":%d,"tipo":"PROJETO","idReferencia":5,
                                 "data":"%s","horasTrabalhadas":2.50,"descricaoAtividade":"Protótipo"}
                                """.formatted(admin.getId(), LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(put("/apontamentos-horas/{id}/validar", apontamentoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"VALIDADO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDADO"));

        verify(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(Object.class));
    }

    private Long readId(String json) throws Exception {
        return objectMapper.readTree(json).get("id").asLong();
    }
}