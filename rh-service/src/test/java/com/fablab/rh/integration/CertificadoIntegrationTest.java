package com.fablab.rh.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.rh.config.RabbitMqConfig;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.RegistroPontoDiario;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CertificadoIntegrationTest extends BaseIntegrationTest {

    private RegistroPontoDiario registrarPresenca(Funcionario funcionario, LocalDate data, String horas) {
        RegistroPontoDiario registro = new RegistroPontoDiario();
        registro.setFuncionario(funcionario);
        registro.setData(data);
        registro.setTotalHoras(new BigDecimal(horas));
        return pontoRepository.save(registro);
    }

    /** Cria apontamento válido (VALIDADO) para o funcionário na data informada. */
    private Long apontamentoValido(Funcionario funcionario, String tokenBolsista, String adminToken,
                                   LocalDate data) throws Exception {
        registrarPresenca(funcionario, data, "8.00");

        String created = mockMvc.perform(post("/apontamentos-horas")
                        .header("Authorization", "Bearer " + tokenBolsista)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idFuncionario":%d,"tipo":"ENCOMENDA","idReferencia":50,
                                 "data":"%s","horasTrabalhadas":3.00}
                                """.formatted(funcionario.getId(), data)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long apontamentoId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(created).get("id").asLong();

        mockMvc.perform(put("/apontamentos-horas/{id}/validar", apontamentoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"VALIDADO"}
                                """))
                .andExpect(status().isOk());
        return apontamentoId;
    }

    @Test
    void funcionarioSolicitaComHorasDisponiveisEAdminAprova() throws Exception {
        Pessoa pessoa = seedPessoa("Maria", "MAT-CERT-1");
        Funcionario funcionario = seedFuncionario(pessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        String bolsistaToken = token(pessoa.getId(), NivelAcesso.BOLSISTA, funcionario.getId());

        Pessoa adminPessoa = seedPessoa("Admin", "MAT-CERT-ADM1");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        Long apontamentoId = apontamentoValido(funcionario, bolsistaToken, adminToken, LocalDate.now());

        mockMvc.perform(get("/horas/disponiveis")
                        .header("Authorization", "Bearer " + bolsistaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idFuncionario").value(funcionario.getId()))
                .andExpect(jsonPath("$.totalHorasDisponiveis").value(3.00));

        var solicitacaoResponse = mockMvc.perform(post("/certificados/solicitar")
                        .header("Authorization", "Bearer " + bolsistaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoCertificado":"COMPLEMENTAR","horasSolicitadas":2.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.idFuncionario").value(funcionario.getId()))
                .andReturn().getResponse().getContentAsString();
        Long solicitacaoId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(solicitacaoResponse).get("idSolicitacao").asLong();

        var emitido = mockMvc.perform(put("/certificados/solicitacoes/{id}/aprovar", solicitacaoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSolicitacao").value(solicitacaoId))
                .andExpect(jsonPath("$.horasCertificadas").value(2.00))
                .andExpect(jsonPath("$.codigoVerificacao").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        Long certificadoId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(emitido).get("idCertificado").asLong();

        var apontamento = apontamentoRepository.findById(apontamentoId).orElseThrow();
        assertThat(apontamento.getConsolidado()).isTrue();

        mockMvc.perform(get("/certificados/emitidos/{id}", certificadoId)
                        .header("Authorization", "Bearer " + bolsistaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCertificado").value(certificadoId))
                .andExpect(jsonPath("$.horas[0].idApontamento").value(apontamentoId))
                .andExpect(jsonPath("$.horas[0].horas").value(3.00));

        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfig.RH_EXCHANGE),
                eq(RabbitMqConfig.CERTIFICADO_SOLICITADO_ROUTING_KEY), (Object) any());
        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfig.RH_EXCHANGE),
                eq(RabbitMqConfig.CERTIFICADO_APROVADO_ROUTING_KEY), (Object) any());
    }

    @Test
    void solicitacaoExcedeHorasDisponiveis() throws Exception {
        Pessoa pessoa = seedPessoa("Maria", "MAT-CERT-2");
        Funcionario funcionario = seedFuncionario(pessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        String bolsistaToken = token(pessoa.getId(), NivelAcesso.BOLSISTA, funcionario.getId());

        mockMvc.perform(post("/certificados/solicitar")
                        .header("Authorization", "Bearer " + bolsistaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoCertificado":"EXTENSAO","horasSolicitadas":5.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("excedem")));
    }

    @Test
    void apenasAdminPodeAprovarSolicitacao() throws Exception {
        Pessoa pessoa = seedPessoa("Maria", "MAT-CERT-3");
        Funcionario funcionario = seedFuncionario(pessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        String bolsistaToken = token(pessoa.getId(), NivelAcesso.BOLSISTA, funcionario.getId());

        Pessoa adminPessoa = seedPessoa("Admin", "MAT-CERT-ADM3");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        apontamentoValido(funcionario, bolsistaToken, adminToken, LocalDate.now());

        var solicitacaoResponse = mockMvc.perform(post("/certificados/solicitar")
                        .header("Authorization", "Bearer " + bolsistaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoCertificado":"ESTAGIO","horasSolicitadas":1.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long solicitacaoId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(solicitacaoResponse).get("idSolicitacao").asLong();

        mockMvc.perform(put("/certificados/solicitacoes/{id}/aprovar", solicitacaoId)
                        .header("Authorization", "Bearer " + bolsistaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/certificados/solicitacoes/{id}/rejeitar", solicitacaoId)
                        .header("Authorization", "Bearer " + bolsistaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void funcionarioVeApenasSuasSolicitacoes() throws Exception {
        Pessoa pessoa1 = seedPessoa("Maria", "MAT-CERT-4");
        Funcionario funcionario1 = seedFuncionario(pessoa1, NivelAcesso.BOLSISTA, "Eletrônica");
        String token1 = token(pessoa1.getId(), NivelAcesso.BOLSISTA, funcionario1.getId());

        Pessoa pessoa2 = seedPessoa("João", "MAT-CERT-5");
        Funcionario funcionario2 = seedFuncionario(pessoa2, NivelAcesso.VOLUNTARIO, "Marcenaria");
        String token2 = token(pessoa2.getId(), NivelAcesso.VOLUNTARIO, funcionario2.getId());

        Pessoa adminPessoa = seedPessoa("Admin", "MAT-CERT-ADM4");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        LocalDate data = LocalDate.now();
        apontamentoValido(funcionario1, token1, adminToken, data);
        apontamentoValido(funcionario2, token2, adminToken, data);

        mockMvc.perform(post("/certificados/solicitar")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoCertificado":"COMPLEMENTAR","horasSolicitadas":2.00}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/certificados/solicitar")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoCertificado":"COMPLEMENTAR","horasSolicitadas":1.00}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/certificados/solicitacoes")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idFuncionario").value(funcionario1.getId()));

        mockMvc.perform(get("/certificados/solicitacoes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void adminRejeitaSolicitacaoComObservacao() throws Exception {
        Pessoa pessoa = seedPessoa("Maria", "MAT-CERT-6");
        Funcionario funcionario = seedFuncionario(pessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        String bolsistaToken = token(pessoa.getId(), NivelAcesso.BOLSISTA, funcionario.getId());

        Pessoa adminPessoa = seedPessoa("Admin", "MAT-CERT-ADM6");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        apontamentoValido(funcionario, bolsistaToken, adminToken, LocalDate.now());

        var solicitacaoResponse = mockMvc.perform(post("/certificados/solicitar")
                        .header("Authorization", "Bearer " + bolsistaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoCertificado":"EXTENSAO","horasSolicitadas":2.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long solicitacaoId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(solicitacaoResponse).get("idSolicitacao").asLong();

        mockMvc.perform(put("/certificados/solicitacoes/{id}/rejeitar", solicitacaoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"observacao":"Dados insuficientes"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADO"))
                .andExpect(jsonPath("$.observacao").value("Dados insuficientes"));

        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfig.RH_EXCHANGE),
                eq(RabbitMqConfig.CERTIFICADO_REJEITADO_ROUTING_KEY), (Object) any());
    }

    @Test
    void employeeNaoVeCertificadoDeOutroFuncionario() throws Exception {
        Pessoa pessoa1 = seedPessoa("Maria", "MAT-CERT-7");
        Funcionario funcionario1 = seedFuncionario(pessoa1, NivelAcesso.BOLSISTA, "Eletrônica");
        String token1 = token(pessoa1.getId(), NivelAcesso.BOLSISTA, funcionario1.getId());

        Pessoa pessoa2 = seedPessoa("João", "MAT-CERT-8");
        Funcionario funcionario2 = seedFuncionario(pessoa2, NivelAcesso.VOLUNTARIO, "Marcenaria");
        String token2 = token(pessoa2.getId(), NivelAcesso.VOLUNTARIO, funcionario2.getId());

        Pessoa adminPessoa = seedPessoa("Admin", "MAT-CERT-ADM8");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        LocalDate data = LocalDate.now();
        apontamentoValido(funcionario1, token1, adminToken, data);

        var solicitacaoResponse = mockMvc.perform(post("/certificados/solicitar")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoCertificado":"COMPLEMENTAR","horasSolicitadas":2.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long solicitacaoId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(solicitacaoResponse).get("idSolicitacao").asLong();

        var emitido = mockMvc.perform(put("/certificados/solicitacoes/{id}/aprovar", solicitacaoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long certificadoId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(emitido).get("idCertificado").asLong();

        mockMvc.perform(get("/certificados/emitidos/{id}", certificadoId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden());

        // Admin pode consultar qualquer certificado.
        mockMvc.perform(get("/certificados/emitidos/{id}", certificadoId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCertificado").value(certificadoId));
    }

    @Test
    void naoPodeAprovarDuasVezesAMesmaSolicitacao() throws Exception {
        Pessoa pessoa = seedPessoa("Maria", "MAT-CERT-9");
        Funcionario funcionario = seedFuncionario(pessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        String bolsistaToken = token(pessoa.getId(), NivelAcesso.BOLSISTA, funcionario.getId());

        Pessoa adminPessoa = seedPessoa("Admin", "MAT-CERT-ADM9");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        apontamentoValido(funcionario, bolsistaToken, adminToken, LocalDate.now());

        var solicitacaoResponse = mockMvc.perform(post("/certificados/solicitar")
                        .header("Authorization", "Bearer " + bolsistaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoCertificado":"ESTAGIO","horasSolicitadas":2.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long solicitacaoId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(solicitacaoResponse).get("idSolicitacao").asLong();

        mockMvc.perform(put("/certificados/solicitacoes/{id}/aprovar", solicitacaoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/certificados/solicitacoes/{id}/aprovar", solicitacaoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("já decidida")));

        verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMqConfig.RH_EXCHANGE),
                eq(RabbitMqConfig.CERTIFICADO_APROVADO_ROUTING_KEY), (Object) any());
    }
}