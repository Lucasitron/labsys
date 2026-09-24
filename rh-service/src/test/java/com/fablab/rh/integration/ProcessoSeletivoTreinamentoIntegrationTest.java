package com.fablab.rh.integration;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.fablab.rh.entity.StatusProcesso;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ProcessoSeletivoTreinamentoIntegrationTest extends BaseIntegrationTest {

    @Test
    void tutorIniciaProcessoSeletivoECriaCandidatoRecrutando() throws Exception {
        Pessoa tutorPessoa = seedPessoa("Prof. Ana", "MAT-TUT");
        Funcionario tutor = seedFuncionario(tutorPessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        seedTutor(tutor);
        String tutorToken = token(tutorPessoa.getId(), NivelAcesso.BOLSISTA, tutor.getId());

        String response = mockMvc.perform(post("/processo-seletivo")
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCompleto":"Candidato Carlinhos","matricula":"MAT-CAND-01",
                                 "contato":"c@c.com","turno":"Noite","idTutor":%d}
                                """.formatted(tutor.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusProcesso").value("INSCRITO"))
                .andReturn().getResponse().getContentAsString();

        Long id = new ObjectMapper().readTree(response).get("id").asLong();
        Long candidatoId = new ObjectMapper()
                .readTree(response).get("idCandidato").asLong();

        var candidato = pessoaRepository.findById(candidatoId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(candidato.getStatus()).isEqualTo(PessoaStatus.RECRUTANDO);
        var funcionario = funcionarioRepository.findByPessoaId(candidatoId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(funcionario.getNivelAcesso()).isEqualTo(NivelAcesso.RECRUTANDO);

        mockMvc.perform(put("/processo-seletivo/{id}", id)
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"statusProcesso":"APROVADO","resultadoFinal":"Aprovado na entrevista"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusProcesso").value("APROVADO"));
    }

    @Test
    void naoTutorNaoPodeIniciarProcessoSeletivo() throws Exception {
        Pessoa pessoa = seedPessoa("Maria", "MAT-300");
        Funcionario funcionario = seedFuncionario(pessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        String bolsistaToken = token(pessoa.getId(), NivelAcesso.BOLSISTA, funcionario.getId());

        Pessoa tutorPessoa = seedPessoa("Prof. Ana", "MAT-TUT-NON");
        Funcionario tutorFunc = seedFuncionario(tutorPessoa, NivelAcesso.VOLUNTARIO, "Marcenaria");
        seedTutor(tutorFunc);

        mockMvc.perform(post("/processo-seletivo")
                        .header("Authorization", "Bearer " + bolsistaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeCompleto":"Candidato X","matricula":"MAT-CAND-02","idTutor":%d}
                                """.formatted(tutorFunc.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void tutorCriaTreinamentoEAvaliacaAluno() throws Exception {
        Pessoa tutorPessoa = seedPessoa("Prof. Ana", "MAT-TUT2");
        Funcionario tutor = seedFuncionario(tutorPessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        seedTutor(tutor);
        String tutorToken = token(tutorPessoa.getId(), NivelAcesso.BOLSISTA, tutor.getId());

        Pessoa aluno = seedPessoa("Maria", "MAT-301");
        Funcionario alunoFunc = seedFuncionario(aluno, NivelAcesso.ESTAGIARIO, "Eletrônica");

        String treinamento = mockMvc.perform(post("/treinamentos")
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Segurança em Impressoras 3D","descricao":"Guia básico",
                                 "urlConteudo":"https://lab/guia-3d"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Segurança em Impressoras 3D"))
                .andReturn().getResponse().getContentAsString();

        Long treinamentoId = new ObjectMapper()
                .readTree(treinamento).get("id").asLong();

        mockMvc.perform(post("/treinamentos/{id}/avaliacoes", treinamentoId)
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idFuncionario":%d,"nota":8.5,"feedback":"Ótimo desempenho","dataAvaliacao":"2026-03-01"}
                                """.formatted(alunoFunc.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nota").value(8.5))
                .andExpect(jsonPath("$.nomeAluno").value("Maria"));

        mockMvc.perform(get("/treinamentos/{id}/avaliacoes", treinamentoId)
                        .header("Authorization", "Bearer " + tutorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void estadoDoProcessoAvanca() throws Exception {
        Pessoa tutorPessoa = seedPessoa("Prof. Ana", "MAT-TUT3");
        Funcionario tutor = seedFuncionario(tutorPessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        seedTutor(tutor);
        String tutorToken = token(tutorPessoa.getId(), NivelAcesso.BOLSISTA, tutor.getId());

        var processo = new com.fablab.rh.entity.ProcessoSeletivo();
        processo.setCandidato(seedPessoa("Cand", "MAT-CAND-03", PessoaStatus.RECRUTANDO));
        processo.setTutor(tutor);
        processo.setStatusProcesso(StatusProcesso.INSCRITO);
        processo.setDataInscricao(java.time.LocalDate.now());
        var salvo = processoRepository.save(processo);

        mockMvc.perform(put("/processo-seletivo/{id}", salvo.getId())
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"statusProcesso":"ENTREVISTA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusProcesso").value("ENTREVISTA"));
    }
}