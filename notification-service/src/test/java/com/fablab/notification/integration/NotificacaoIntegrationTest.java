package com.fablab.notification.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.NivelAcesso;
import com.fablab.notification.entity.Notificacao;
import com.fablab.notification.entity.StatusNotificacao;
import com.fablab.notification.entity.TipoEvento;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Fluxo completo de criação, leitura, revisão e configuração de canal. */
class NotificacaoIntegrationTest extends BaseIntegrationTest {

    @Test
    void adminEnviaNotificacaoDeTeste() throws Exception {
        mockMvc.perform(post("/notificacoes/teste")
                        .header("Authorization", bearer(5L, NivelAcesso.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idDestinatario":1,"canal":"EMAIL","assunto":"Oi","mensagem":"Tudo bem?"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idDestinatario").value(1))
                .andExpect(jsonPath("$.tipoEvento").value("TESTE"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        assertThat(notificacaoRepository.findAll()).hasSize(1);
    }

    @Test
    void testeComCanalHabilitadoEnviaEmail() throws Exception {
        criarConfiguracao(CanalNotificacao.EMAIL, true);

        mockMvc.perform(post("/notificacoes/teste")
                        .header("Authorization", bearer(5L, NivelAcesso.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idDestinatario":1,"canal":"EMAIL","assunto":"Oi","mensagem":"Tudo bem?"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ENVIADA"));

        assertThat(notificacaoRepository.findAll().get(0).getDataEnvio()).isNotNull();
    }

    @Test
    void usuarioListaApenasSuasNotificacoes() throws Exception {
        criarNotificacao(1L, StatusNotificacao.PENDENTE, TipoEvento.TESTE);
        criarNotificacao(2L, StatusNotificacao.PENDENTE, TipoEvento.TESTE);

        mockMvc.perform(get("/notificacoes").header("Authorization", bearer(1L, NivelAcesso.BOLSISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idDestinatario").value(1));
    }

    @Test
    void usuarioListaComFiltroDeStatus() throws Exception {
        criarNotificacao(1L, StatusNotificacao.PENDENTE, TipoEvento.TESTE);
        criarNotificacao(1L, StatusNotificacao.LIDA, TipoEvento.TESTE);

        mockMvc.perform(get("/notificacoes").param("status", "LIDA")
                        .header("Authorization", bearer(1L, NivelAcesso.BOLSISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("LIDA"));
    }

    @Test
    void donoMarcaNotificacaoComoLida() throws Exception {
        Notificacao notificacao = criarNotificacao(1L, StatusNotificacao.ENVIADA, TipoEvento.TESTE);

        mockMvc.perform(put("/notificacoes/" + notificacao.getIdNotificacao() + "/ler")
                        .header("Authorization", bearer(1L, NivelAcesso.BOLSISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIDA"));

        assertThat(notificacaoRepository.findById(notificacao.getIdNotificacao()))
                .get().extracting(Notificacao::getDataLeitura).isNotNull();
    }

    @Test
    void outroUsuarioNaoMarcaNotificacaoAlheiaComoLida() throws Exception {
        Notificacao notificacao = criarNotificacao(1L, StatusNotificacao.ENVIADA, TipoEvento.TESTE);

        mockMvc.perform(put("/notificacoes/" + notificacao.getIdNotificacao() + "/ler")
                        .header("Authorization", bearer(2L, NivelAcesso.BOLSISTA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void notificacaoInexistenteRetorna404() throws Exception {
        mockMvc.perform(put("/notificacoes/999/ler")
                        .header("Authorization", bearer(1L, NivelAcesso.BOLSISTA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminRevisaNotificacoesMovendoParaHistorico() throws Exception {
        Notificacao notificacao = criarNotificacao(1L, StatusNotificacao.LIDA, TipoEvento.TESTE);

        mockMvc.perform(post("/notificacoes/admin/revisar")
                        .header("Authorization", bearer(5L, NivelAcesso.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idsNotificacoes\":[" + notificacao.getIdNotificacao() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisadas").value(1));

        assertThat(notificacaoRepository.findAll()).isEmpty();
        assertThat(historicoRepository.findAll()).hasSize(1);
        assertThat(historicoRepository.findAll().get(0).getIdAdminRevisor()).isEqualTo(5L);
    }

    @Test
    void adminListaHistorico() throws Exception {
        Notificacao notificacao = criarNotificacao(1L, StatusNotificacao.LIDA, TipoEvento.TESTE);
        mockMvc.perform(post("/notificacoes/admin/revisar")
                .header("Authorization", bearer(5L, NivelAcesso.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idsNotificacoes\":[" + notificacao.getIdNotificacao() + "]}"));

        mockMvc.perform(get("/notificacoes/historico").header("Authorization", bearer(5L, NivelAcesso.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void adminListaTodasAsNotificacoesComFiltro() throws Exception {
        criarNotificacao(1L, StatusNotificacao.PENDENTE, TipoEvento.TESTE);
        criarNotificacao(2L, StatusNotificacao.PENDENTE, TipoEvento.ESTOQUE_BAIXO);

        mockMvc.perform(get("/notificacoes/admin").param("tipoEvento", "ESTOQUE_BAIXO")
                        .header("Authorization", bearer(5L, NivelAcesso.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void revisaoDeIdsVaziosRetorna400() throws Exception {
        mockMvc.perform(post("/notificacoes/admin/revisar")
                        .header("Authorization", bearer(5L, NivelAcesso.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idsNotificacoes\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminListaEAtualizaConfiguracoesDeCanal() throws Exception {
        Long id = criarConfiguracao(CanalNotificacao.EMAIL, true).getIdConfiguracao();

        mockMvc.perform(get("/configuracoes-canal").header("Authorization", bearer(5L, NivelAcesso.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(put("/configuracoes-canal/" + id)
                        .header("Authorization", bearer(5L, NivelAcesso.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"habilitado\":false,\"parametros\":\"{}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitado").value(false));
    }

    @Test
    void configuracaoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/configuracoes-canal/999")
                        .header("Authorization", bearer(5L, NivelAcesso.ADMIN)))
                .andExpect(status().isNotFound());
    }
}
