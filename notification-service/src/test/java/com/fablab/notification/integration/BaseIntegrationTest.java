package com.fablab.notification.integration;

import com.fablab.notification.TokenHelper;
import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.ConfiguracaoCanal;
import com.fablab.notification.entity.NivelAcesso;
import com.fablab.notification.entity.Notificacao;
import com.fablab.notification.entity.StatusNotificacao;
import com.fablab.notification.entity.TipoEvento;
import com.fablab.notification.repository.ConfiguracaoCanalRepository;
import com.fablab.notification.repository.NotificacaoHistoricoRepository;
import com.fablab.notification.repository.NotificacaoRepository;
import java.time.LocalDateTime;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base para testes de integração: contexto completo com banco H2 e infra de
 * mensageria/e-mail substituída por mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
public abstract class BaseIntegrationTest {

    @MockBean
    protected RabbitTemplate rabbitTemplate;

    @MockBean
    protected JavaMailSender javaMailSender;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected NotificacaoRepository notificacaoRepository;

    @Autowired
    protected NotificacaoHistoricoRepository historicoRepository;

    @Autowired
    protected ConfiguracaoCanalRepository configuracaoCanalRepository;

    protected String bearer(Long idPessoa, NivelAcesso nivel) {
        return "Bearer " + TokenHelper.token(idPessoa, nivel);
    }

    protected String bearerComRole(Long idPessoa, String role) {
        return "Bearer " + TokenHelper.tokenWithRole(idPessoa, role);
    }

    protected Notificacao criarNotificacao(Long idDestinatario, StatusNotificacao status, TipoEvento tipo) {
        Notificacao notificacao = new Notificacao();
        notificacao.setIdDestinatario(idDestinatario);
        notificacao.setCanal(CanalNotificacao.EMAIL);
        notificacao.setTipoEvento(tipo);
        notificacao.setAssunto("Assunto de teste");
        notificacao.setMensagem("Mensagem de teste");
        notificacao.setStatus(status);
        notificacao.setDataCriacao(LocalDateTime.now());
        if (status == StatusNotificacao.ENVIADA || status == StatusNotificacao.LIDA) {
            notificacao.setDataEnvio(LocalDateTime.now());
        }
        if (status == StatusNotificacao.LIDA) {
            notificacao.setDataLeitura(LocalDateTime.now());
        }
        return notificacaoRepository.save(notificacao);
    }

    protected ConfiguracaoCanal criarConfiguracao(CanalNotificacao canal, boolean habilitado) {
        ConfiguracaoCanal configuracao = new ConfiguracaoCanal();
        configuracao.setCanal(canal);
        configuracao.setHabilitado(habilitado);
        return configuracaoCanalRepository.save(configuracao);
    }
}
