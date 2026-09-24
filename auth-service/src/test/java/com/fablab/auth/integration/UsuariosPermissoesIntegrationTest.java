package com.fablab.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.auth.entity.Login;
import com.fablab.auth.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Cobre o despacho AUTH-USUARIOS-001: {@code PUT /auth/senha},
 * {@code /api/usuarios} (CRUD + filtros + counts) e {@code /api/permissoes}
 * (matriz + validação de enums), com RBAC Admin server-side (C-1), erros PT
 * sem detalhe técnico (C-4) e counts servidos (C-6).
 */
class UsuariosPermissoesIntegrationTest extends BaseIntegrationTest {

    private Login admin;
    private Login bolsista;

    @BeforeEach
    void seed() {
        admin = seedUser(100L, Role.ADMIN, "CARD-ADMIN", "admin@fablab.io", "admin", "Direção");
        bolsista = seedUser(101L, Role.BOLSISTA, "CARD-BOLSISTA", "bolsista@fablab.io", "bolsista", "Laboratório");
    }

    // PUT /auth/senha

    @Test
    void alterarSenhaComDadosValidosRetornaSucesso() throws Exception {
        String token = accessToken(bolsista, Role.BOLSISTA);

        mockMvc.perform(put("/auth/senha")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"senhaAtual":"%s","novaSenha":"NovaSenha@456","confirmacaoSenha":"NovaSenha@456"}
                                """.formatted(SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Senha alterada com sucesso"));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"bolsista@fablab.io","senha":"NovaSenha@456"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void alterarSenhaComSenhaAtualErradaRetornaNaoAutorizadoPt() throws Exception {
        String token = accessToken(bolsista, Role.BOLSISTA);

        mockMvc.perform(put("/auth/senha")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"senhaAtual":"Errada@123","novaSenha":"NovaSenha@456","confirmacaoSenha":"NovaSenha@456"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Senha atual incorreta"));
    }

    @Test
    void alterarSenhaCurtaRetornaErroPt() throws Exception {
        String token = accessToken(bolsista, Role.BOLSISTA);

        mockMvc.perform(put("/auth/senha")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"senhaAtual":"%s","novaSenha":"curta","confirmacaoSenha":"curta"}
                                """.formatted(SENHA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alterarSenhaComConfirmacaoDivergenteRetornaErroPt() throws Exception {
        String token = accessToken(bolsista, Role.BOLSISTA);

        mockMvc.perform(put("/auth/senha")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"senhaAtual":"%s","novaSenha":"NovaSenha@456","confirmacaoSenha":"Outra@789"}
                                """.formatted(SENHA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nova senha e confirmação não conferem"));
    }

    @Test
    void alterarSenhaSemTokenRetornaNaoAutorizado() throws Exception {
        mockMvc.perform(put("/auth/senha")
                        .contentType("application/json")
                        .content("""
                                {"senhaAtual":"x","novaSenha":"NovaSenha@456","confirmacaoSenha":"NovaSenha@456"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // GET /api/usuarios (C-1, C-6)

    @Test
    void adminListaUsuariosComPaginacaoECounts() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(get("/api/usuarios?page=0&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.counts.total").value(2))
                .andExpect(jsonPath("$.counts.ativos").value(2))
                .andExpect(jsonPath("$.counts.porNivel.ADMIN").value(1))
                .andExpect(jsonPath("$.counts.porNivel.BOLSISTA").value(1));
    }

    @Test
    void adminFiltraUsuariosPorSearchNivelSituacao() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(get("/api/usuarios?search=bolsista&nivel=1&situacao=Ativo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("bolsista@fablab.io"))
                .andExpect(jsonPath("$.counts.total").value(1));
    }

    @Test
    void naoAdminNaoListaUsuarios() throws Exception {
        String token = accessToken(bolsista, Role.BOLSISTA);

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado"));
    }

    @Test
    void anonimoNaoListaUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    // POST/PUT/PATCH /api/usuarios

    @Test
    void adminCriaAtualizaEDesativaUsuario() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        String created = mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"idUser":102,"email":"novo@fablab.io","nomeUsuario":"novo","senha":"SenhaNova@123","nivel":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacao").value("Pendente"))
                .andExpect(jsonPath("$.nivel").value("VOLUNTARIO"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(put("/api/usuarios/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"nivel":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivel").value("BOLSISTA"));

        mockMvc.perform(patch("/api/usuarios/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"situacao":"Desativado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("Desativado"));
    }

    @Test
    void criarUsuarioDuplicadoRetornaErroPt() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"idUser":103,"email":"admin@fablab.io","nomeUsuario":"outro","senha":"SenhaNova@123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("E-mail já cadastrado"));
    }

    @Test
    void atualizarUsuarioInexistenteRetorna404Pt() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(put("/api/usuarios/99999")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"setor":"Laboratório"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuário não encontrado"));
    }

    @Test
    void naoAdminNaoCriaUsuario() throws Exception {
        String token = accessToken(bolsista, Role.BOLSISTA);

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"idUser":104,"email":"x@fablab.io","nomeUsuario":"x","senha":"SenhaNova@123"}
                                """))
                .andExpect(status().isForbidden());
    }

    // /api/permissoes (C-1, C-3, C-4)

    @Test
    void adminObtemMatrizCompleta() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(get("/api/permissoes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles.length()").value(5))
                .andExpect(jsonPath("$.matriz").isArray())
                .andExpect(jsonPath("$.enums.modulos").isArray());
    }

    @Test
    void adminAtualizaCelulaValida() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(put("/api/permissoes/estoque/BOLSISTA")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"valor":"Editar"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modulo").value("estoque"))
                .andExpect(jsonPath("$.nivel").value("BOLSISTA"))
                .andExpect(jsonPath("$.valor").value("Editar"));
    }

    @Test
    void atualizarCelulaComModuloInvalidoRetorna422Pt() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(put("/api/permissoes/invalido/ADMIN")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"valor":"Ver"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void atualizarCelulaComNivelInvalidoRetorna422Pt() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(put("/api/permissoes/rh/9")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"valor":"Ver"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void atualizarCelulaComValorInvalidoRetorna422Pt() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(put("/api/permissoes/rh/ADMIN")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"valor":"Apagar"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void naoAdminNaoAcessaPermissoes() throws Exception {
        String token = accessToken(bolsista, Role.BOLSISTA);

        mockMvc.perform(get("/api/permissoes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/permissoes/rh/ADMIN")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"valor":"Ver"}
                                """))
                .andExpect(status().isForbidden());
    }
}
