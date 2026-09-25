package com.fablab.rh.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Níveis e acesso (D-8): matriz, alteração de membro e convites.
 */
class NiveisIntegrationTest extends BaseIntegrationTest {

    @Test
    void matrizComContagensEPermissoes() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM-NIV");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        Pessoa ana = seedPessoa("Ana Souza", "MAT-080");
        seedFuncionario(ana, NivelAcesso.VOLUNTARIO, "Mecatrônica");

        mockMvc.perform(get("/niveis")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.niveis.length()").value(5))
                .andExpect(jsonPath("$.niveis[0].id").value("admin"))
                .andExpect(jsonPath("$.niveis[2].membros").value(1))
                .andExpect(jsonPath("$.permissoes.length()").value(9));
    }

    @Test
    void alteraMembroComHistoricoEEvento() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM-NIV2");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        Pessoa ana = seedPessoa("Ana Souza", "MAT-081");
        Funcionario anaFunc = seedFuncionario(ana, NivelAcesso.BOLSISTA, "Mecatrônica");

        mockMvc.perform(patch("/niveis/{id}/membros", anaFunc.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nivel":"VOLUNTARIO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelNovo").value("VOLUNTARIO"));

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void conviteCriaPessoaEVinculo() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM-NIV3");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        mockMvc.perform(post("/niveis/convites")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCompleto":"Novo Membro","matricula":"MAT-082",
                                 "departamento":"Mecatrônica","nivelAcesso":"VOLUNTARIO"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nivelAcesso").value("VOLUNTARIO"))
                .andExpect(jsonPath("$.nomePessoa").value("Novo Membro"));
    }

    @Test
    void niveisExigemAdmin() throws Exception {
        Pessoa ana = seedPessoa("Ana Souza", "MAT-083");
        Funcionario bolsista = seedFuncionario(ana, NivelAcesso.BOLSISTA, "Mecatrônica");
        String token = token(ana.getId(), NivelAcesso.BOLSISTA, bolsista.getId());

        mockMvc.perform(get("/niveis")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/niveis/convites")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCompleto":"Outro","matricula":"MAT-084"}
                                """))
                .andExpect(status().isForbidden());
    }
}
