package com.fablab.rh.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.PessoaStatus;
import org.junit.jupiter.api.Test;

/**
 * Lista paginada de pessoas (D-1/D-3): filtros, facetas e escopo por nível.
 */
class PessoaListagemIntegrationTest extends BaseIntegrationTest {

    @Test
    void adminListaComPaginacaoEFiltros() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM-LIST");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        Pessoa ana = seedPessoa("Ana Souza", "MAT-010");
        seedFuncionario(ana, NivelAcesso.VOLUNTARIO, "Mecatrônica");
        Pessoa bruno = seedPessoa("Bruno Nunes", "MAT-011");
        seedFuncionario(bruno, NivelAcesso.BOLSISTA, "Eletrônica");

        mockMvc.perform(get("/pessoas")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoas.length()").value(3))
                .andExpect(jsonPath("$.paginacao.page").value(1))
                .andExpect(jsonPath("$.paginacao.totalItems").value(3))
                .andExpect(jsonPath("$.filtros.statuses.length()").value(1))
                .andExpect(jsonPath("$.filtros.niveis.length()").value(3));

        mockMvc.perform(get("/pessoas")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoas.length()").value(1))
                .andExpect(jsonPath("$.pessoas[0].matricula").value("MAT-010"));

        mockMvc.perform(get("/pessoas")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("nivel", "BOLSISTA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoas.length()").value(1))
                .andExpect(jsonPath("$.pessoas[0].matricula").value("MAT-011"));

        mockMvc.perform(get("/pessoas")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("setor", "mecatrônica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoas.length()").value(1))
                .andExpect(jsonPath("$.pessoas[0].matricula").value("MAT-010"));

        mockMvc.perform(get("/pessoas")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "2")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoas.length()").value(1))
                .andExpect(jsonPath("$.paginacao.totalPages").value(2));
    }

    @Test
    void estagiarioVeApenasOProprioRegistro() throws Exception {
        Pessoa ana = seedPessoa("Ana Souza", "MAT-020");
        Funcionario estagiaria = seedFuncionario(ana, NivelAcesso.ESTAGIARIO, "Mecatrônica");
        String token = token(ana.getId(), NivelAcesso.ESTAGIARIO, estagiaria.getId());
        seedPessoa("Bruno Nunes", "MAT-021");

        mockMvc.perform(get("/pessoas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoas.length()").value(1))
                .andExpect(jsonPath("$.pessoas[0].matricula").value("MAT-020"));

        mockMvc.perform(get("/pessoas")
                        .header("Authorization", "Bearer " + token)
                        .param("search", "bruno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoas.length()").value(0));
    }

    @Test
    void recrutandoNaoAcessaOModulo() throws Exception {
        Pessoa recruta = seedPessoa("Recruta", "MAT-022", PessoaStatus.RECRUTANDO);
        Funcionario func = seedFuncionario(recruta, NivelAcesso.RECRUTANDO, null);
        String token = token(recruta.getId(), NivelAcesso.RECRUTANDO, func.getId());

        mockMvc.perform(get("/pessoas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void bolsistaVeDetalheDeOutroRegistro() throws Exception {
        Pessoa ana = seedPessoa("Ana Souza", "MAT-030");
        Funcionario bolsista = seedFuncionario(ana, NivelAcesso.BOLSISTA, "Mecatrônica");
        String token = token(ana.getId(), NivelAcesso.BOLSISTA, bolsista.getId());
        Pessoa bruno = seedPessoa("Bruno Nunes", "MAT-031");

        mockMvc.perform(get("/pessoas/{id}", bruno.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricula").value("MAT-031"));
    }

    @Test
    void estagiarioNaoVeDetalheDeOutroRegistro() throws Exception {
        Pessoa ana = seedPessoa("Ana Souza", "MAT-040");
        Funcionario estagiaria = seedFuncionario(ana, NivelAcesso.ESTAGIARIO, "Mecatrônica");
        String token = token(ana.getId(), NivelAcesso.ESTAGIARIO, estagiaria.getId());
        Pessoa bruno = seedPessoa("Bruno Nunes", "MAT-041");

        mockMvc.perform(get("/pessoas/{id}", bruno.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/pessoas/{id}", ana.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
