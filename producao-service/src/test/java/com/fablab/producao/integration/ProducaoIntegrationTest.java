package com.fablab.producao.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.producao.entity.NivelAcesso;
import com.fablab.producao.entity.Projeto;
import com.fablab.producao.entity.ProjetoStatus;
import com.fablab.producao.entity.Setor;
import com.fablab.producao.entity.SetorChecklist;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ProducaoIntegrationTest extends BaseIntegrationTest {

    private static final String CONTENT = MediaType.APPLICATION_JSON_VALUE;

    private Projeto seedProjeto() {
        Projeto projeto = new Projeto();
        projeto.setNome("Braço Robótico");
        projeto.setDataInicio(LocalDate.now());
        projeto.setIdResponsavel(1L);
        projeto.setStatus(ProjetoStatus.PLANEJADO);
        return projetoRepository.save(projeto);
    }

    private Setor seedSetor() {
        Setor setor = new Setor();
        setor.setNumero(1);
        setor.setNome("Marcenaria");
        setor.setAtivo(true);
        return setorRepository.save(setor);
    }

    private SetorChecklist seedChecklist(Setor setor) {
        SetorChecklist checklist = new SetorChecklist();
        checklist.setSetor(setor);
        checklist.setItem("Bancada limpa");
        checklist.setAtivo(true);
        return checklistRepository.save(checklist);
    }

    @Test
    void fluxoDeProjetosETarefas() throws Exception {
        String token = bearer(1L, NivelAcesso.ADMIN);

        String body = mockMvc.perform(post("/projetos")
                        .header("Authorization", token).contentType(CONTENT)
                        .content("""
                                {"nome":"Braço Robótico","dataInicio":"%s","dataFimPrevista":"%s","idResponsavel":1}
                                """.formatted(LocalDate.now(), LocalDate.now().plusMonths(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLANEJADO"))
                .andReturn().getResponse().getContentAsString();
        long idProjeto = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(body).get("idProjeto").asLong();

        mockMvc.perform(put("/projetos/" + idProjeto + "/status")
                        .header("Authorization", token).contentType(CONTENT)
                        .content("""
                                {"status":"EM_ANDAMENTO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));

        mockMvc.perform(post("/tarefas")
                        .header("Authorization", token).contentType(CONTENT)
                        .content("""
                                {"idProjeto":%d,"titulo":"Montar","prioridade":"ALTA","idResponsavel":1}
                                """.formatted(idProjeto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        mockMvc.perform(get("/tarefas").param("idProjeto", String.valueOf(idProjeto))
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void fluxoDeKanban() throws Exception {
        String token = bearer(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/kanban")
                        .header("Authorization", token).contentType(CONTENT)
                        .content("""
                                {"idEncomenda":100,"idResponsavel":1,"ordem":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILA"));

        var kanban = kanbanRepository.findByIdEncomenda(100L).orElseThrow();

        mockMvc.perform(put("/kanban/" + kanban.getIdKanban() + "/mover")
                        .header("Authorization", token).contentType(CONTENT)
                        .content("""
                                {"statusNovo":"ENTREGUE","observacao":"finalizado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"));

        mockMvc.perform(get("/kanban/encomenda/100/historico").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void fluxoDeMaquinas() throws Exception {
        String token = bearer(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/maquinas")
                        .header("Authorization", token).contentType(CONTENT)
                        .content("""
                                {"nome":"Impressora 3D","localizacao":"Sala 2"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DISPONIVEL"));

        var maquina = maquinaRepository.findAll().get(0);

        String usoBody = mockMvc.perform(post("/maquinas/" + maquina.getIdMaquina() + "/uso")
                        .header("Authorization", token).contentType(CONTENT)
                        .content("""
                                {"idFuncionario":1}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long idUso = new com.fasterxml.jackson.databind.ObjectMapper().readTree(usoBody).get("idUso").asLong();

        mockMvc.perform(put("/maquinas/" + maquina.getIdMaquina() + "/uso/" + idUso + "/fim")
                        .header("Authorization", token).contentType(CONTENT)
                        .content("""
                                {"observacao":"ok"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasUso").exists());

        mockMvc.perform(get("/maquinas/" + maquina.getIdMaquina() + "/historico")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void fluxoDeInspecao5SComNaoConformidade() throws Exception {
        Setor setor = seedSetor();
        SetorChecklist checklist = seedChecklist(setor);
        String token = bearer(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/setores/" + setor.getIdSetor() + "/responsaveis")
                        .header("Authorization", token).contentType(CONTENT)
                        .content("""
                                {"idFuncionario":9,"dataInicio":"%s","ativo":true}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/inspecoes-5s")
                        .header("Authorization", token).contentType(CONTENT)
                        .content("""
                                {"idSetor":%d,"idInspetor":1,"dataInspecao":"%s","turno":"MANHA",
                                 "itens":[{"idChecklist":%d,"conforme":false,"observacao":"sujo"}]}
                                """.formatted(setor.getIdSetor(), LocalDate.now(), checklist.getIdChecklist())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NAO_CONFORME"));

        mockMvc.perform(get("/advertencias/9").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].contador").value(1));
    }

    @Test
    void fluxoDeProjetoMesaComQrCodeEAuditoria() throws Exception {
        String token = bearer(1L, NivelAcesso.ADMIN);

        String body = mockMvc.perform(post("/projetos-mesa")
                        .header("Authorization", token).contentType(CONTENT)
                        .content("""
                                {"idFuncionario":5,"idMesa":1,"nomeProjeto":"Drone","tipoProjeto":"Aeroespacial"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qrCodeTotem").exists())
                .andReturn().getResponse().getContentAsString();
        long id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("idProjetoMesa").asLong();

        mockMvc.perform(get("/projetos-mesa/" + id + "/qrcode").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentType(MediaType.IMAGE_PNG));

        mockMvc.perform(put("/projetos-mesa/" + id + "/evolucao").header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auditorias-projeto-mesa")
                        .header("Authorization", token).contentType(CONTENT)
                        .content("""
                                {"idProjetoMesa":%d,"resultado":"ABANDONADO","acaoTomada":"Recolher"}
                                """.formatted(id)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/projetos-mesa/" + id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABANDONADO"));
    }

    @Test
    void parametros5SPodemSerListadosEAlteradosPeloAdmin() throws Exception {
        String token = bearer(1L, NivelAcesso.ADMIN);
        var parametro = new com.fablab.producao.entity.Parametro5S();
        parametro.setChave("rotacaoDias");
        parametro.setValor("7");
        parametroRepository.save(parametro);

        String body = mockMvc.perform(get("/parametros-5s").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get(0).get("idParametro").asLong();

        mockMvc.perform(put("/parametros-5s/" + id)
                        .header("Authorization", token).contentType(CONTENT)
                        .content("""
                                {"valor":"30","descricao":"atualizado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value("30"));
    }

    @Test
    void criarProjetoInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/projetos")
                        .header("Authorization", bearer(1L, NivelAcesso.ADMIN)).contentType(CONTENT)
                        .content("""
                                {"nome":"","dataInicio":"%s","idResponsavel":1}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recursoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/projetos/9999").header("Authorization", bearer(1L, NivelAcesso.ADMIN)))
                .andExpect(status().isNotFound());
    }
}