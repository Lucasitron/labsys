package com.fablab.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.auth.entity.Login;
import com.fablab.auth.entity.Role;
import com.fablab.auth.entity.TokenIntegracao;
import com.fablab.auth.repository.TokenIntegracaoRepository;
import com.fablab.auth.service.TokenIntegracaoService;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Cobre o despacho AUTH-CONFIG-SISTEMA: {@code /api/configuracoes/sistema}
 * (GET/PUT identidade + cadências, 422 PT se inválido) e
 * {@code /api/configuracoes/tokens} (lista sem segredo, criação com chave
 * 1× + só hash armazenado, revogação soft + 404), tudo Admin-only
 * server-side (C-5).
 */
class ConfigSistemaTokensIntegrationTest extends BaseIntegrationTest {

    private Login admin;
    private Login bolsista;

    @Autowired
    private TokenIntegracaoRepository tokenRepository;

    @BeforeEach
    void seed() {
        admin = seedUser(200L, Role.ADMIN, "CARD-ADMIN-CFG", "admin.cfg@fablab.io", "admincfg", "Direção");
        bolsista = seedUser(201L, Role.BOLSISTA, "CARD-BOLSISTA-CFG", "bolsista.cfg@fablab.io", "bolsistacfg",
                "Laboratório");
    }

    // GET /api/configuracoes/sistema

    @Test
    void adminObtemSistemaComDefaults() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(get("/api/configuracoes/sistema")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identidade.nomeFablab").value("FabLab IFPR — Curitiba"))
                .andExpect(jsonPath("$.cadenciaChecklist5S").value("Semanal"))
                .andExpect(jsonPath("$.cadenciaAuditoria5S").value("Mensal"))
                .andExpect(jsonPath("$.tokens").isArray())
                .andExpect(jsonPath("$.tokens").isEmpty());
    }

    @Test
    void adminAtualizaSistemaEPersiste() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(put("/api/configuracoes/sistema")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"identidade":{"nomeFablab":"FabLab Teste","logo":"https://exemplo/logo.png"},
                                 "cadenciaChecklist5S":"Quinzenal","cadenciaAuditoria5S":"Mensal"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identidade.nomeFablab").value("FabLab Teste"))
                .andExpect(jsonPath("$.cadenciaChecklist5S").value("Quinzenal"));

        mockMvc.perform(get("/api/configuracoes/sistema")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identidade.nomeFablab").value("FabLab Teste"))
                .andExpect(jsonPath("$.cadenciaChecklist5S").value("Quinzenal"));
    }

    @Test
    void atualizarSistemaComNomeEmBrancoRetorna422Pt() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(put("/api/configuracoes/sistema")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"identidade":{"nomeFablab":"  "},"cadenciaChecklist5S":"Semanal"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void atualizarSistemaComCadenciaInvalidaRetorna422Pt() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(put("/api/configuracoes/sistema")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"identidade":{"nomeFablab":"FabLab"},"cadenciaChecklist5S":"Anual"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        "Cadência do checklist 5S inválida: use Semanal, Quinzenal ou Mensal"));
    }

    @Test
    void naoAdminNaoAcessaSistema() throws Exception {
        String token = accessToken(bolsista, Role.BOLSISTA);

        mockMvc.perform(get("/api/configuracoes/sistema")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/configuracoes/sistema")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"identidade":{"nomeFablab":"X"},"cadenciaChecklist5S":"Semanal"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonimoNaoAcessaSistema() throws Exception {
        mockMvc.perform(get("/api/configuracoes/sistema"))
                .andExpect(status().isUnauthorized());
    }

    // /api/configuracoes/tokens (C-5)

    @Test
    void criarTokenExibeChaveUmaVezEArmazenaSoHash() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        String created = mockMvc.perform(post("/api/configuracoes/tokens")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Impressora 3D — API"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nome").value("Impressora 3D — API"))
                .andExpect(jsonPath("$.prefixo").exists())
                .andExpect(jsonPath("$.chave").exists())
                .andExpect(jsonPath("$.hash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String chave = com.jayway.jsonpath.JsonPath.read(created, "$.chave");
        Number id = com.jayway.jsonpath.JsonPath.read(created, "$.id");

        // Armazenado: só hash + prefixo; hash ≠ chave.
        TokenIntegracao salvo = tokenRepository.findById(id.longValue()).orElseThrow();
        assertThat(salvo.getHash()).isNotEqualTo(chave);
        assertThat(salvo.getHash()).isEqualTo(sha256(chave));
        assertThat(salvo.getPrefixo()).isEqualTo(chave.substring(0, 12));
        assertThat(salvo.isRevogado()).isFalse();

        // Lista ativa: sem segredo, sem reexpor a chave.
        mockMvc.perform(get("/api/configuracoes/tokens")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.longValue()))
                .andExpect(jsonPath("$[0].nome").value("Impressora 3D — API"))
                .andExpect(jsonPath("$[0].prefixo").exists())
                .andExpect(jsonPath("$[0].criadoEm").exists())
                .andExpect(jsonPath("$[0].revogado").value(false))
                .andExpect(jsonPath("$[0].chave").doesNotExist())
                .andExpect(jsonPath("$[0].hash").doesNotExist());
    }

    @Test
    void criarTokenDuplicadoRetorna422Pt() throws Exception {
        String token = accessToken(admin, Role.ADMIN);
        String body = """
                {"nome":"Rede Wi-Fi"}
                """;

        mockMvc.perform(post("/api/configuracoes/tokens")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/configuracoes/tokens")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Já existe um token ativo com este nome"));
    }

    @Test
    void criarTokenSemNomeRetorna422Pt() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(post("/api/configuracoes/tokens")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"  "}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void revogarTokenSomeDaListaAtivaEInexistenteRetorna404() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        String created = mockMvc.perform(post("/api/configuracoes/tokens")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Validador externo"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(created, "$.id");

        mockMvc.perform(delete("/api/configuracoes/tokens/" + id.longValue())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Revogado some da lista ativa (soft revogado=true).
        mockMvc.perform(get("/api/configuracoes/tokens")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        assertThat(tokenRepository.findById(id.longValue()).orElseThrow().isRevogado()).isTrue();

        mockMvc.perform(delete("/api/configuracoes/tokens/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Token de integração não encontrado"));
    }

    @Test
    void naoAdminNaoAcessaTokens() throws Exception {
        String token = accessToken(bolsista, Role.BOLSISTA);

        mockMvc.perform(get("/api/configuracoes/tokens")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/configuracoes/tokens")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"nome":"X"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/configuracoes/tokens/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private static String sha256(String texto) throws Exception {
        Method metodo = TokenIntegracaoService.class.getDeclaredMethod("sha256Hex", String.class);
        metodo.setAccessible(true);
        return (String) metodo.invoke(null, texto);
    }
}
