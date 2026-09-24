package com.fablab.rh.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.rh.entity.ApontamentoHoras;
import com.fablab.rh.entity.AvaliacaoTreinamento;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.RegistroPontoDiario;
import com.fablab.rh.entity.StatusApontamento;
import com.fablab.rh.entity.TipoApontamento;
import com.fablab.rh.entity.Treinamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Detalhe agregado da pessoa (D-2) e exclusão (Extra).
 */
class PessoaDetalheExclusaoIntegrationTest extends BaseIntegrationTest {

    @Test
    void detalheAgregaAbas() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM-DET");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        Pessoa ana = seedPessoa("Ana Souza", "MAT-050");
        Funcionario anaFunc = seedFuncionario(ana, NivelAcesso.VOLUNTARIO, "Mecatrônica");

        ApontamentoHoras apontamento = new ApontamentoHoras();
        apontamento.setFuncionario(anaFunc);
        apontamento.setTipo(TipoApontamento.PROJETO);
        apontamento.setIdReferencia(7L);
        apontamento.setData(LocalDate.now());
        apontamento.setHorasTrabalhadas(new BigDecimal("3.00"));
        apontamento.setStatus(StatusApontamento.VALIDADO);
        apontamentoRepository.save(apontamento);

        Treinamento treinamento = new Treinamento();
        treinamento.setTitulo("Impressora 3D");
        treinamento.setTutor(admin);
        treinamentoRepository.save(treinamento);

        AvaliacaoTreinamento avaliacao = new AvaliacaoTreinamento();
        avaliacao.setTreinamento(treinamento);
        avaliacao.setFuncionario(anaFunc);
        avaliacao.setNota(new BigDecimal("8.00"));
        avaliacao.setDataAvaliacao(LocalDate.now());
        avaliacaoRepository.save(avaliacao);

        RegistroPontoDiario registro = new RegistroPontoDiario();
        registro.setFuncionario(anaFunc);
        registro.setData(LocalDate.now());
        registro.setHoraEntrada(Instant.now().minusSeconds(8 * 3600));
        registro.setHoraSaida(Instant.now());
        registro.setTotalHoras(new BigDecimal("8.00"));
        pontoRepository.save(registro);

        mockMvc.perform(get("/pessoas/{id}/detalhe", ana.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoa.matricula").value("MAT-050"))
                .andExpect(jsonPath("$.idFuncionario").value(anaFunc.getId()))
                .andExpect(jsonPath("$.nivel").value("VOLUNTARIO"))
                .andExpect(jsonPath("$.horasMes.atual").value(3.0))
                .andExpect(jsonPath("$.treinamentos.concluidos").value(1))
                .andExpect(jsonPath("$.treinamentos.media").value(8.0))
                .andExpect(jsonPath("$.horasPorStatus.validadas").value(1))
                .andExpect(jsonPath("$.avaliacoes.length()").value(1))
                .andExpect(jsonPath("$.avaliacoes[0].nota").value(8.0));
    }

    @Test
    void detalheSemVinculoDegradaParaVazio() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM-DET2");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());

        Pessoa semVinculo = seedPessoa("Sem Vinculo", "MAT-051");

        mockMvc.perform(get("/pessoas/{id}/detalhe", semVinculo.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idFuncionario").doesNotExist())
                .andExpect(jsonPath("$.treinamentos.concluidos").value(0))
                .andExpect(jsonPath("$.avaliacoes.length()").value(0));
    }

    @Test
    void detalheRespeitaEscopoProprio() throws Exception {
        Pessoa ana = seedPessoa("Ana Souza", "MAT-052");
        Funcionario estagiaria = seedFuncionario(ana, NivelAcesso.ESTAGIARIO, "Mecatrônica");
        String token = token(ana.getId(), NivelAcesso.ESTAGIARIO, estagiaria.getId());
        Pessoa bruno = seedPessoa("Bruno Nunes", "MAT-053");

        mockMvc.perform(get("/pessoas/{id}/detalhe", bruno.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/pessoas/{id}/detalhe", ana.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void excluirPessoaSemVinculo() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM-DEL");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());
        Pessoa alvo = seedPessoa("Alvo", "MAT-054");

        mockMvc.perform(delete("/pessoas/{id}", alvo.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        mockMvc.perform(get("/pessoas/{id}", alvo.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void excluirPessoaComVinculoRetorna409() throws Exception {
        Pessoa adminPessoa = seedPessoa("Admin", "MAT-ADM-DEL2");
        Funcionario admin = seedFuncionario(adminPessoa, NivelAcesso.ADMIN, "Coordenação");
        String adminToken = token(adminPessoa.getId(), NivelAcesso.ADMIN, admin.getId());
        Pessoa vinculada = seedPessoa("Vinculada", "MAT-055");
        seedFuncionario(vinculada, NivelAcesso.BOLSISTA, "Eletrônica");

        mockMvc.perform(delete("/pessoas/{id}", vinculada.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void excluirExigeAdmin() throws Exception {
        Pessoa ana = seedPessoa("Ana Souza", "MAT-056");
        Funcionario bolsista = seedFuncionario(ana, NivelAcesso.BOLSISTA, "Mecatrônica");
        String token = token(ana.getId(), NivelAcesso.BOLSISTA, bolsista.getId());
        Pessoa alvo = seedPessoa("Alvo", "MAT-057");

        mockMvc.perform(delete("/pessoas/{id}", alvo.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
