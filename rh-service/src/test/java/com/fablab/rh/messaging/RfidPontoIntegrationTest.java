package com.fablab.rh.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fablab.rh.dto.RfidAccessEvent;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.integration.BaseIntegrationTest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class RfidPontoIntegrationTest extends BaseIntegrationTest {

    private Instant dia(LocalDateTime local) {
        return local.atZone(ZoneId.systemDefault()).toInstant();
    }

    @Test
    void entradaESaidaGeramRegistroDePontoComTotalDeHoras() {
        Pessoa pessoa = seedPessoa("Maria", "MAT-RFID-1");
        Funcionario funcionario = seedFuncionario(pessoa, NivelAcesso.BOLSISTA, "Eletrônica");

        pontoService.processarEventoRfid(new RfidAccessEvent(
                pessoa.getId(), "ABC-123", dia(LocalDateTime.of(2026, 1, 5, 8, 0)), "ENTRADA"));
        pontoService.processarEventoRfid(new RfidAccessEvent(
                pessoa.getId(), "ABC-123", dia(LocalDateTime.of(2026, 1, 5, 17, 30)), "SAIDA"));

        var registro = pontoRepository.findByFuncionarioIdAndData(
                        funcionario.getId(), java.time.LocalDate.of(2026, 1, 5))
                .orElseThrow();

        assertThat(registro.getHoraEntrada()).isNotNull();
        assertThat(registro.getHoraSaida()).isNotNull();
        assertThat(registro.getTotalHoras()).isEqualByComparingTo("9.50");
    }

    @Test
    void eventoDeAcessoNegadoNaoGeraRegistro() {
        Pessoa pessoa = seedPessoa("Maria", "MAT-RFID-2");
        Funcionario funcionario = seedFuncionario(pessoa, NivelAcesso.BOLSISTA, "Eletrônica");

        pontoService.processarEventoRfid(new RfidAccessEvent(
                pessoa.getId(), "ABC-124", Instant.now(), "ACESSO_NEGADO"));

        assertThat(pontoRepository.findByFuncionarioIdAndData(
                funcionario.getId(), java.time.LocalDate.now())).isEmpty();
    }

    @Test
    void eventoDeUsuarioSemFuncionarioEhIgnorado() {
        Pessoa pessoa = seedPessoa("Só Pessoa", "MAT-RFID-3");

        pontoService.processarEventoRfid(new RfidAccessEvent(
                pessoa.getId(), "ABC-125", Instant.now(), "ENTRADA"));

        assertThat(pontoRepository.findByFuncionarioIdAndData(
                1L, java.time.LocalDate.now())).isEmpty();
    }
}