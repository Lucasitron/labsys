package com.fablab.rh.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.PessoaStatus;
import com.fablab.rh.entity.ProcessoSeletivo;
import com.fablab.rh.entity.StatusProcesso;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Processo seletivo (D-4): grupos, etapa individual e avaliação pelo tutor.
 */
class ProcessoSeletivoGruposIntegrationTest extends BaseIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fluxoDeGrupoEstagioEAvaliacao() throws Exception {
        Pessoa tutorPessoa = seedPessoa("Prof. Ana", "MAT-TUT-GRP");
        Funcionario tutor = seedFuncionario(tutorPessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        seedTutor(tutor);
        String tutorToken = token(tutorPessoa.getId(), NivelAcesso.BOLSISTA, tutor.getId());

        Pessoa cand1 = seedPessoa("Cand Um", "MAT-GRP-01", PessoaStatus.RECRUTANDO);
        ProcessoSeletivo proc1 = novoProcesso(cand1, tutor);
        Pessoa cand2 = seedPessoa("Cand Dois", "MAT-GRP-02", PessoaStatus.RECRUTANDO);
        novoProcesso(cand2, tutor);

        String grupo = mockMvc.perform(post("/processo-seletivo/grupos")
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Eletrônica","idLider":%d,"membroIds":[%d,%d]}
                                """.formatted(tutor.getId(), cand1.getId(), cand2.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Eletrônica"))
                .andExpect(jsonPath("$.totalMembros").value(2))
                .andReturn().getResponse().getContentAsString();
        long grupoId = objectMapper.readTree(grupo).get("id").asLong();

        mockMvc.perform(patch("/processo-seletivo/{id}/estagio", proc1.getId())
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etapa":"ENTREVISTA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusProcesso").value("ENTREVISTA"));

        mockMvc.perform(post("/processo-seletivo/{id}/membros/{pessoaId}/avaliar",
                        proc1.getId(), cand1.getId())
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nota":8.5,"feedback":"Boa comunicação"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nota").value(8.5))
                .andExpect(jsonPath("$.feedback").value("Boa comunicação"))
                .andExpect(jsonPath("$.idGrupo").value(grupoId));

        mockMvc.perform(get("/processo-seletivo")
                        .header("Authorization", "Bearer " + tutorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupos.length()").value(1))
                .andExpect(jsonPath("$.grupos[0].candidatos.length()").value(2))
                .andExpect(jsonPath("$.totais.inscritos").value(2))
                .andExpect(jsonPath("$.totais.grupos").value(1));

        mockMvc.perform(get("/processo-seletivo")
                        .header("Authorization", "Bearer " + tutorToken)
                        .param("estagio", "ENTREVISTA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupos[0].candidatos.length()").value(1));
    }

    @Test
    void tutorDeOutroProcessoNaoAltera() throws Exception {
        Pessoa tutorPessoa = seedPessoa("Prof. Ana", "MAT-TUT-GRP2");
        Funcionario tutor = seedFuncionario(tutorPessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        seedTutor(tutor);

        Pessoa outroTutorPessoa = seedPessoa("Prof. Beto", "MAT-TUT-GRP3");
        Funcionario outroTutor = seedFuncionario(outroTutorPessoa, NivelAcesso.BOLSISTA, "Mecatrônica");
        seedTutor(outroTutor);
        String outroToken = token(outroTutorPessoa.getId(), NivelAcesso.BOLSISTA, outroTutor.getId());

        Pessoa cand = seedPessoa("Cand Tres", "MAT-GRP-03", PessoaStatus.RECRUTANDO);
        ProcessoSeletivo proc = novoProcesso(cand, tutor);

        mockMvc.perform(patch("/processo-seletivo/{id}/estagio", proc.getId())
                        .header("Authorization", "Bearer " + outroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etapa":"APROVADO"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void avaliarExigeNotaNaFaixa() throws Exception {
        Pessoa tutorPessoa = seedPessoa("Prof. Ana", "MAT-TUT-GRP4");
        Funcionario tutor = seedFuncionario(tutorPessoa, NivelAcesso.BOLSISTA, "Eletrônica");
        seedTutor(tutor);
        String tutorToken = token(tutorPessoa.getId(), NivelAcesso.BOLSISTA, tutor.getId());

        Pessoa cand = seedPessoa("Cand Quatro", "MAT-GRP-04", PessoaStatus.RECRUTANDO);
        ProcessoSeletivo proc = novoProcesso(cand, tutor);

        mockMvc.perform(post("/processo-seletivo/{id}/membros/{pessoaId}/avaliar",
                        proc.getId(), cand.getId())
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nota":11.0}
                                """))
                .andExpect(status().isBadRequest());
    }

    private ProcessoSeletivo novoProcesso(Pessoa candidato, Funcionario tutor) {
        ProcessoSeletivo processo = new ProcessoSeletivo();
        processo.setCandidato(candidato);
        processo.setTutor(tutor);
        processo.setStatusProcesso(StatusProcesso.INSCRITO);
        processo.setDataInscricao(LocalDate.now());
        return processoRepository.save(processo);
    }
}
