package com.fablab.notification.service;

import com.fablab.notification.config.NotificationProperties;
import com.fablab.notification.dto.NotificacaoHistoricoResponse;
import com.fablab.notification.dto.NotificacaoResponse;
import com.fablab.notification.dto.NotificacaoTesteRequest;
import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.Notificacao;
import com.fablab.notification.entity.NotificacaoHistorico;
import com.fablab.notification.entity.StatusNotificacao;
import com.fablab.notification.entity.TipoEvento;
import com.fablab.notification.exception.ForbiddenException;
import com.fablab.notification.exception.ResourceNotFoundException;
import com.fablab.notification.repository.NotificacaoHistoricoRepository;
import com.fablab.notification.repository.NotificacaoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Regras de negócio das notificações: criação (inclusive a partir de eventos),
 * envio pelo canal configurado, leitura pelo destinatário, revisão pelo Admin e
 * expurgo do histórico.
 */
@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    private final NotificacaoRepository notificacaoRepository;
    private final NotificacaoHistoricoRepository historicoRepository;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final ConfiguracaoCanalService configuracaoCanalService;
    private final AcessoService acessoService;
    private final NotificationProperties properties;

    public NotificacaoService(NotificacaoRepository notificacaoRepository,
                              NotificacaoHistoricoRepository historicoRepository,
                              EmailService emailService,
                              WhatsAppService whatsAppService,
                              ConfiguracaoCanalService configuracaoCanalService,
                              AcessoService acessoService,
                              NotificationProperties properties) {
        this.notificacaoRepository = notificacaoRepository;
        this.historicoRepository = historicoRepository;
        this.emailService = emailService;
        this.whatsAppService = whatsAppService;
        this.configuracaoCanalService = configuracaoCanalService;
        this.acessoService = acessoService;
        this.properties = properties;
    }

    /** Registra uma notificação usando o canal EMAIL por padrão. */
    @Transactional
    public NotificacaoResponse registrar(Long idDestinatario, TipoEvento tipoEvento,
                                         String assunto, String mensagem, Long idReferencia) {
        return registrar(idDestinatario, CanalNotificacao.EMAIL, tipoEvento, assunto, mensagem, idReferencia);
    }

    /**
     * Registra uma notificação, persiste como {@code PENDENTE} e tenta enviá-la
     * imediatamente. Em caso de falha, permanece {@code PENDENTE}.
     */
    @Transactional
    public NotificacaoResponse registrar(Long idDestinatario, CanalNotificacao canal, TipoEvento tipoEvento,
                                         String assunto, String mensagem, Long idReferencia) {
        Notificacao notificacao = new Notificacao();
        notificacao.setIdDestinatario(idDestinatario);
        notificacao.setCanal(canal);
        notificacao.setTipoEvento(tipoEvento);
        notificacao.setAssunto(assunto);
        notificacao.setMensagem(mensagem);
        notificacao.setIdReferencia(idReferencia);
        notificacao.setStatus(StatusNotificacao.PENDENTE);
        notificacao.setDataCriacao(LocalDateTime.now());
        Notificacao salva = notificacaoRepository.save(notificacao);
        tentarEnviar(salva);
        return NotificacaoResponse.from(salva);
    }

    /** Tenta enviar a notificação pelo canal configurado, atualizando o status. */
    @Transactional
    public void tentarEnviar(Notificacao notificacao) {
        boolean enviada = switch (notificacao.getCanal()) {
            case EMAIL -> emailService.enviar(notificacao);
            case WHATSAPP -> whatsAppService.enviar(notificacao);
        };
        if (enviada) {
            notificacao.setStatus(StatusNotificacao.ENVIADA);
            notificacao.setDataEnvio(LocalDateTime.now());
            notificacaoRepository.save(notificacao);
        } else {
            log.debug("Notificação {} permanece pendente", notificacao.getIdNotificacao());
        }
    }

    @Transactional(readOnly = true)
    public List<NotificacaoResponse> listarDoUsuario(StatusNotificacao status, TipoEvento tipoEvento) {
        Long id = acessoService.idUsuario();
        if (id == null) {
            throw new ForbiddenException("Usuário não autenticado");
        }
        List<Notificacao> notificacoes;
        if (status != null && tipoEvento != null) {
            notificacoes = notificacaoRepository
                    .findByIdDestinatarioAndStatusAndTipoEventoOrderByDataCriacaoDesc(id, status, tipoEvento);
        } else if (status != null) {
            notificacoes = notificacaoRepository.findByIdDestinatarioAndStatusOrderByDataCriacaoDesc(id, status);
        } else if (tipoEvento != null) {
            notificacoes = notificacaoRepository
                    .findByIdDestinatarioAndTipoEventoOrderByDataCriacaoDesc(id, tipoEvento);
        } else {
            notificacoes = notificacaoRepository.findByIdDestinatarioOrderByDataCriacaoDesc(id);
        }
        return notificacoes.stream().map(NotificacaoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificacaoResponse> listarAdmin(StatusNotificacao status, TipoEvento tipoEvento) {
        List<Notificacao> notificacoes;
        if (status != null && tipoEvento != null) {
            notificacoes = notificacaoRepository
                    .findByStatusAndTipoEventoOrderByDataCriacaoDesc(status, tipoEvento);
        } else if (status != null) {
            notificacoes = notificacaoRepository.findByStatusOrderByDataCriacaoDesc(status);
        } else if (tipoEvento != null) {
            notificacoes = notificacaoRepository.findByTipoEventoOrderByDataCriacaoDesc(tipoEvento);
        } else {
            notificacoes = notificacaoRepository.findAllByOrderByDataCriacaoDesc();
        }
        return notificacoes.stream().map(NotificacaoResponse::from).toList();
    }

    /** Marca uma notificação do próprio usuário como lida. */
    @Transactional
    public NotificacaoResponse marcarComoLida(Long id) {
        Notificacao notificacao = obter(id);
        if (!acessoService.podeAcessar(notificacao.getIdDestinatario())) {
            throw new ForbiddenException("Você só pode marcar suas próprias notificações como lidas");
        }
        if (notificacao.getStatus() != StatusNotificacao.LIDA) {
            notificacao.setStatus(StatusNotificacao.LIDA);
            notificacao.setDataLeitura(LocalDateTime.now());
        }
        return NotificacaoResponse.from(notificacao);
    }

    /**
     * Move as notificações selecionadas para o histórico, gravando a data de
     * revisão e o Admin responsável.
     */
    @Transactional
    public int revisar(List<Long> ids) {
        Long idAdmin = acessoService.idUsuario();
        LocalDateTime agora = LocalDateTime.now();
        for (Long id : ids) {
            Notificacao notificacao = obter(id);
            HistoricoBuilder builder = new HistoricoBuilder(notificacao, agora, idAdmin);
            historicoRepository.save(builder.build());
            notificacaoRepository.delete(notificacao);
        }
        return ids.size();
    }

    @Transactional(readOnly = true)
    public List<NotificacaoHistoricoResponse> listarHistorico(CanalNotificacao canal, TipoEvento tipoEvento) {
        List<NotificacaoHistorico> historicos;
        if (canal != null && tipoEvento != null) {
            historicos = historicoRepository.findByCanalAndTipoEventoOrderByDataRevisaoAdminDesc(canal, tipoEvento);
        } else if (canal != null) {
            historicos = historicoRepository.findByCanalOrderByDataRevisaoAdminDesc(canal);
        } else if (tipoEvento != null) {
            historicos = historicoRepository.findByTipoEventoOrderByDataRevisaoAdminDesc(tipoEvento);
        } else {
            historicos = historicoRepository.findAllByOrderByDataRevisaoAdminDesc();
        }
        return historicos.stream().map(NotificacaoHistoricoResponse::from).toList();
    }

    /** Envia uma notificação de teste (uso do Admin). */
    @Transactional
    public NotificacaoResponse enviarTeste(NotificacaoTesteRequest request) {
        Long destino = request.idDestinatario() != null ? request.idDestinatario() : acessoService.idUsuario();
        CanalNotificacao canal = request.canal() != null ? request.canal() : CanalNotificacao.EMAIL;
        String assunto = StringUtils.hasText(request.assunto())
                ? request.assunto() : "Teste de notificação";
        String mensagem = StringUtils.hasText(request.mensagem())
                ? request.mensagem() : "Notificação de teste do Notification Service";
        return registrar(destino, canal, TipoEvento.TESTE, assunto, mensagem, null);
    }

    /** Remove definitivamente históricos revisados há mais que a retenção. */
    @Transactional
    public int expurgarHistorico() {
        LocalDateTime limite = LocalDateTime.now().minusDays(properties.retencaoDias());
        long removidos = historicoRepository.deleteByDataRevisaoAdminBefore(limite);
        if (removidos > 0) {
            log.info("Expurgados {} registros de histórico anteriores a {}", removidos, limite);
        }
        return (int) removidos;
    }

    /** Indica se o canal está habilitado. */
    @Transactional(readOnly = true)
    public boolean canalHabilitado(CanalNotificacao canal) {
        return configuracaoCanalService.isHabilitado(canal);
    }

    /**
     * Tenta reenviar todas as notificações pendentes.
     *
     * @return quantidade de notificações processadas
     */
    @Transactional
    public int reenviarPendentes() {
        List<Notificacao> pendentes = notificacaoRepository
                .findByStatusOrderByDataCriacaoDesc(StatusNotificacao.PENDENTE);
        pendentes.forEach(this::tentarEnviar);
        return pendentes.size();
    }

    private Notificacao obter(Long id) {
        return notificacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação", id));
    }

    /** Constrói o registro histórico a partir de uma notificação ativa. */
    private static final class HistoricoBuilder {

        private final Notificacao notificacao;
        private final LocalDateTime dataRevisao;
        private final Long idAdmin;

        private HistoricoBuilder(Notificacao notificacao, LocalDateTime dataRevisao, Long idAdmin) {
            this.notificacao = notificacao;
            this.dataRevisao = dataRevisao;
            this.idAdmin = idAdmin;
        }

        private NotificacaoHistorico build() {
            NotificacaoHistorico historico = new NotificacaoHistorico();
            historico.setIdNotificacaoOriginal(notificacao.getIdNotificacao());
            historico.setIdDestinatario(notificacao.getIdDestinatario());
            historico.setCanal(notificacao.getCanal());
            historico.setTipoEvento(notificacao.getTipoEvento());
            historico.setAssunto(notificacao.getAssunto());
            historico.setMensagem(notificacao.getMensagem());
            historico.setIdReferencia(notificacao.getIdReferencia());
            historico.setDataCriacao(notificacao.getDataCriacao());
            historico.setDataEnvio(notificacao.getDataEnvio());
            historico.setDataLeitura(notificacao.getDataLeitura());
            historico.setDataRevisaoAdmin(dataRevisao);
            historico.setIdAdminRevisor(idAdmin);
            return historico;
        }
    }
}
