package com.fablab.notification.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fablab.notification.dto.ConfiguracaoCanalRequest;
import com.fablab.notification.dto.ConfiguracaoCanalResponse;
import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.ConfiguracaoCanal;
import com.fablab.notification.exception.ResourceNotFoundException;
import com.fablab.notification.repository.ConfiguracaoCanalRepository;
import com.fablab.notification.service.ConfiguracaoCanalService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfiguracaoCanalServiceTest {

    @Mock
    private ConfiguracaoCanalRepository repository;

    @InjectMocks
    private ConfiguracaoCanalService service;

    private ConfiguracaoCanal configuracao() {
        ConfiguracaoCanal configuracao = new ConfiguracaoCanal();
        configuracao.setIdConfiguracao(1L);
        configuracao.setCanal(CanalNotificacao.EMAIL);
        configuracao.setHabilitado(true);
        return configuracao;
    }

    @Test
    void listarRetornaTodasAsConfiguracoes() {
        when(repository.findAll()).thenReturn(List.of(configuracao()));

        List<ConfiguracaoCanalResponse> resposta = service.listar();

        assertThat(resposta).hasSize(1);
        assertThat(resposta.get(0).canal()).isEqualTo(CanalNotificacao.EMAIL);
    }

    @Test
    void buscarInexistenteLancaNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void atualizarAlteraHabilitadoEParametros() {
        ConfiguracaoCanal configuracao = configuracao();
        when(repository.findById(1L)).thenReturn(Optional.of(configuracao));

        ConfiguracaoCanalResponse resposta = service.atualizar(1L,
                new ConfiguracaoCanalRequest(false, "{\"x\":1}"));

        assertThat(resposta.habilitado()).isFalse();
        assertThat(resposta.parametros()).isEqualTo("{\"x\":1}");
    }

    @Test
    void isHabilitadoRetornaTrueQuandoConfigurado() {
        when(repository.findByCanal(CanalNotificacao.EMAIL)).thenReturn(Optional.of(configuracao()));

        assertThat(service.isHabilitado(CanalNotificacao.EMAIL)).isTrue();
    }

    @Test
    void isHabilitadoRetornaFalseQuandoAusente() {
        when(repository.findByCanal(CanalNotificacao.WHATSAPP)).thenReturn(Optional.empty());

        assertThat(service.isHabilitado(CanalNotificacao.WHATSAPP)).isFalse();
    }
}
