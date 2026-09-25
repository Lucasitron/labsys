package com.fablab.rh.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SecurityConfigTest extends BaseIntegrationTest {

    @Test
    void requisicaoSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/pessoas/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void alteracaoDeNivelRejeitaNaoAdmin() throws Exception {
        Pessoa pessoa = seedPessoa("Maria", "MAT-400");
        Funcionario funcionario = seedFuncionario(pessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        String bolsistaToken = token(pessoa.getId(), NivelAcesso.BOLSISTA, funcionario.getId());

        mockMvc.perform(put("/funcionarios/{id}/nivel", funcionario.getId())
                        .header("Authorization", "Bearer " + bolsistaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nivelNovo":"ESTAGIARIO"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void validacaoDeApontamentoRejeitaNaoAdmin() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM-SEC");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");

        Pessoa pessoa = seedPessoa("Maria", "MAT-401");
        Funcionario funcionario = seedFuncionario(pessoa, NivelAcesso.VOLUNTARIO, "Marcenaria");
        String voluntarioToken = token(pessoa.getId(), NivelAcesso.VOLUNTARIO, funcionario.getId());

        var apontamento = new com.fablab.rh.entity.ApontamentoHoras();
        apontamento.setFuncionario(admin);
        apontamento.setTipo(com.fablab.rh.entity.TipoApontamento.PROJETO);
        apontamento.setIdReferencia(1L);
        apontamento.setData(java.time.LocalDate.now());
        apontamento.setHorasTrabalhadas(java.math.BigDecimal.ONE);
        apontamento.setStatus(com.fablab.rh.entity.StatusApontamento.PENDENTE);
        var salvo = apontamentoRepository.save(apontamento);

        mockMvc.perform(put("/apontamentos-horas/{id}/validar", salvo.getId())
                        .header("Authorization", "Bearer " + voluntarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"VALIDADO"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void listagemDeFuncionariosRejeitaNaoAdmin() throws Exception {
        Pessoa pessoa = seedPessoa("Maria", "MAT-402");
        Funcionario funcionario = seedFuncionario(pessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        String bolsistaToken = token(pessoa.getId(), NivelAcesso.BOLSISTA, funcionario.getId());

        mockMvc.perform(get("/funcionarios")
                        .header("Authorization", "Bearer " + bolsistaToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void criacaoDePessoaPermitidaParaAdmin() throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN, 2L);

        mockMvc.perform(post("/pessoas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCompleto":"Admin cria","matricula":"MAT-ADMIN-1"}
                                """))
                .andExpect(status().isCreated());
    }
}