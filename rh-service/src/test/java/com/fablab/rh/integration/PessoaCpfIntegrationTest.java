package com.fablab.rh.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * CPF opcional com máscara LGPD (Extra).
 */
class PessoaCpfIntegrationTest extends BaseIntegrationTest {

    @Test
    void cadastraComCpfERespondeMascarado() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM-CPF");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        String resposta = mockMvc.perform(post("/pessoas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCompleto":"Maria Silva","matricula":"MAT-060",
                                 "cpf":"529.982.247-25"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value("***.***.***-25"))
                .andReturn().getResponse().getContentAsString();

        long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(resposta).get("id").asLong();

        mockMvc.perform(get("/pessoas/{id}", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("***.***.***-25"));
    }

    @Test
    void rejeitaCpfInvalido() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM-CPF2");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        mockMvc.perform(post("/pessoas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCompleto":"Maria Silva","matricula":"MAT-061",
                                 "cpf":"111.111.111-11"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejeitaCpfDuplicado() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM-CPF3");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        seedPessoa("Existente", "MAT-062").setCpf("52998224725");
        pessoaRepository.flush();

        mockMvc.perform(post("/pessoas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCompleto":"Nova Pessoa","matricula":"MAT-063",
                                 "cpf":"52998224725"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CPF já cadastrado"));
    }
}
