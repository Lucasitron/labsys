package com.fablab.vendas.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.vendas.entity.NivelAcesso;
import com.fablab.vendas.entity.TipoPessoa;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ClienteIntegrationTest extends BaseIntegrationTest {

    @Test
    void adminCadastraClientePF() throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/clientes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoPessoa":"PF","nomeRazaoSocial":"João da Silva",
                                 "cpfCnpj":"52998224725","email":"joao@email.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nomeRazaoSocial").value("João da Silva"));
    }

    @Test
    void adminCadastraClientePJComCNPJValido() throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/clientes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoPessoa":"PJ","nomeRazaoSocial":"Empresa X LTDA",
                                 "cpfCnpj":"11222333000181"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoPessoa").value("PJ"));
    }

    @Test
    void cadastroComCPFInvalidoRetorna400() throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/clientes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoPessoa":"PF","nomeRazaoSocial":"Teste",
                                 "cpfCnpj":"12345678900"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cadastroDuplicadoRetorna400() throws Exception {
        seedCliente("Cliente", "52998224725", TipoPessoa.PF);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/clientes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoPessoa":"PF","nomeRazaoSocial":"Outro",
                                 "cpfCnpj":"52998224725"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaClientesComFiltroPorTipo() throws Exception {
        seedCliente("João", "52998224725", TipoPessoa.PF);
        seedCliente("Empresa", "11222333000181", TipoPessoa.PJ);
        String token = token(2L, NivelAcesso.ESTAGIARIO);

        mockMvc.perform(get("/clientes").param("tipo", "PJ")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nomeRazaoSocial").value("Empresa"));
    }

    @Test
    void listaClientesPorTag() throws Exception {
        var cliente = seedCliente("VIP", "52998224725", TipoPessoa.PF);
        var tag = seedTag("VIP", "#ffcc00");
        String token = token(2L, NivelAcesso.ESTAGIARIO);

        // vincula tag via endpoint e valida listagem por tag
        String adminToken = token(1L, NivelAcesso.ADMIN);
        mockMvc.perform(post("/clientes/{id}/tags/{idTag}", cliente.getId(), tag.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[0].nome").value("VIP"));

        mockMvc.perform(get("/clientes").param("idTag", tag.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomeRazaoSocial").value("VIP"));
    }

    @Test
    void buscaClientePorId() throws Exception {
        var cliente = seedCliente("Ana");
        String token = token(2L, NivelAcesso.ESTAGIARIO);

        mockMvc.perform(get("/clientes/{id}", cliente.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeRazaoSocial").value("Ana"));
    }

    @Test
    void criaEListaTags() throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/tags")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"3D","cor":"#00ff00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("3D"));

        mockMvc.perform(get("/tags").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("3D"));
    }

    @Test
    void tagDuplicadaRetorna400() throws Exception {
        seedTag("3D", "#00ff00");
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/tags")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"3D","cor":"#ff0000"}
                                """))
                .andExpect(status().isBadRequest());
    }
}