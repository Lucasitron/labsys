package com.fablab.notification.messaging;

import com.fablab.notification.config.RabbitMqConfig;
import com.fablab.notification.dto.AdvertenciaRegistradaEvent;
import com.fablab.notification.dto.CertificadoAprovadoEvent;
import com.fablab.notification.dto.CertificadoRejeitadoEvent;
import com.fablab.notification.dto.CertificadoSolicitadoEvent;
import com.fablab.notification.dto.CompraSolicitadaEvent;
import com.fablab.notification.dto.EmprestimoAtrasadoEvent;
import com.fablab.notification.dto.EncomendaCriadaEvent;
import com.fablab.notification.dto.EncomendaStatusAlteradoEvent;
import com.fablab.notification.dto.EstoqueBaixoEvent;
import com.fablab.notification.dto.ExtratoMensalHorasEvent;
import com.fablab.notification.dto.HorasValidadasEvent;
import com.fablab.notification.dto.KanbanStatusAlteradoEvent;
import com.fablab.notification.dto.LancamentoVencidoEvent;
import com.fablab.notification.dto.NivelAlteradoEvent;
import com.fablab.notification.dto.OrcamentoAprovadoEvent;
import com.fablab.notification.dto.ProjetoMesaAbandonadoEvent;
import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.TipoEvento;
import com.fablab.notification.service.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome os eventos de domínio publicados pelos demais serviços e cria as
 * notificações correspondentes. Eventos sem destinatário pessoal são
 * encaminhados para revisão do Admin (destinatário nulo).
 */
@Component
public class NotificacaoEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoEventListener.class);

    private final NotificacaoService notificacaoService;

    public NotificacaoEventListener(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @RabbitListener(queues = RabbitMqConfig.ESTOQUE_BAIXO_QUEUE)
    public void onEstoqueBaixo(EstoqueBaixoEvent event) {
        log.info("Evento recebido: estoque.baixo para o item {}", event.idItem());
        notificacaoService.registrar(null, CanalNotificacao.EMAIL, TipoEvento.ESTOQUE_BAIXO,
                "Estoque baixo: " + event.nome(),
                "O item \"" + event.nome() + "\" (id " + event.idItem() + ") está com "
                        + event.quantidadeAtual() + " unidades, abaixo do mínimo de "
                        + event.estoqueMinimo() + ".",
                event.idItem());
    }

    @RabbitListener(queues = RabbitMqConfig.EMPRESTIMO_ATRASADO_QUEUE)
    public void onEmprestimoAtrasado(EmprestimoAtrasadoEvent event) {
        log.info("Evento recebido: emprestimo.atrasado para o empréstimo {}", event.idEmprestimo());
        notificacaoService.registrar(event.idPessoa(), CanalNotificacao.EMAIL, TipoEvento.EMPRESTIMO_ATRASADO,
                "Empréstimo em atraso",
                "O empréstimo " + event.idEmprestimo() + " do item " + event.idItem()
                        + " deveria ter sido devolvido em " + event.dataDevolucaoPrevista() + ".",
                event.idEmprestimo());
    }

    @RabbitListener(queues = RabbitMqConfig.ENCOMENDA_CRIADA_QUEUE)
    public void onEncomendaCriada(EncomendaCriadaEvent event) {
        log.info("Evento recebido: encomenda.criada para a encomenda {}", event.idEncomenda());
        notificacaoService.registrar(event.idCliente(), CanalNotificacao.EMAIL, TipoEvento.ENCOMENDA_CRIADA,
                "Encomenda registrada",
                "Sua encomenda " + event.idEncomenda() + " foi registrada no status "
                        + event.statusKanban() + ", com valor final de " + event.valorFinal() + ".",
                event.idEncomenda());
    }

    @RabbitListener(queues = RabbitMqConfig.ENCOMENDA_STATUS_QUEUE)
    public void onEncomendaStatusAlterado(EncomendaStatusAlteradoEvent event) {
        log.info("Evento recebido: encomenda.status.alterado para a encomenda {}", event.idEncomenda());
        notificacaoService.registrar(event.idCliente(), CanalNotificacao.EMAIL, TipoEvento.ENCOMENDA_STATUS_ALTERADO,
                "Status da encomenda atualizado",
                "Sua encomenda " + event.idEncomenda() + " mudou de " + event.statusAnterior()
                        + " para " + event.statusNovo() + ".",
                event.idEncomenda());
    }

    @RabbitListener(queues = RabbitMqConfig.ORCAMENTO_APROVADO_QUEUE)
    public void onOrcamentoAprovado(OrcamentoAprovadoEvent event) {
        log.info("Evento recebido: orcamento.aprovado para o orçamento {}", event.idOrcamento());
        notificacaoService.registrar(event.idCliente(), CanalNotificacao.EMAIL, TipoEvento.ORCAMENTO_APROVADO,
                "Orçamento aprovado",
                "O orçamento " + event.idOrcamento() + " foi aprovado em " + event.dataAprovacao()
                        + ", no valor total de " + event.valorTotal() + ".",
                event.idOrcamento());
    }

    @RabbitListener(queues = RabbitMqConfig.LANCAMENTO_VENCIDO_QUEUE)
    public void onLancamentoVencido(LancamentoVencidoEvent event) {
        log.info("Evento recebido: lancamento.vencido para o lançamento {}", event.idLancamento());
        notificacaoService.registrar(null, CanalNotificacao.EMAIL, TipoEvento.LANCAMENTO_VENCIDO,
                "Lançamento vencido",
                "O lançamento " + event.idLancamento() + " do tipo " + event.tipo()
                        + " (valor " + event.valor() + ") venceu em " + event.dataVencimento() + ".",
                event.idLancamento());
    }

    @RabbitListener(queues = RabbitMqConfig.COMPRA_SOLICITADA_QUEUE)
    public void onCompraSolicitada(CompraSolicitadaEvent event) {
        log.info("Evento recebido: compra.solicitada para a compra {}", event.idCompra());
        notificacaoService.registrar(null, CanalNotificacao.EMAIL, TipoEvento.COMPRA_SOLICITADA,
                "Compra solicitada",
                "A compra " + event.idCompra() + " foi solicitada ao fornecedor "
                        + event.idFornecedor() + ".",
                event.idCompra());
    }

    @RabbitListener(queues = RabbitMqConfig.ADVERTENCIA_QUEUE)
    public void onAdvertenciaRegistrada(AdvertenciaRegistradaEvent event) {
        log.info("Evento recebido: advertencia.registrada para o funcionário {}", event.idFuncionario());
        notificacaoService.registrar(event.idFuncionario(), CanalNotificacao.EMAIL, TipoEvento.ADVERTENCIA_REGISTRADA,
                "Advertência registrada",
                "Uma nova advertência (" + event.contador() + "ª) foi registrada. Motivo: "
                        + event.motivo() + ".",
                event.idFuncionario());
    }

    @RabbitListener(queues = RabbitMqConfig.PROJETO_MESA_QUEUE)
    public void onProjetoMesaAbandonado(ProjetoMesaAbandonadoEvent event) {
        log.info("Evento recebido: projeto.mesa.abandonado para o projeto {}", event.idProjetoMesa());
        notificacaoService.registrar(event.idFuncionario(), CanalNotificacao.EMAIL, TipoEvento.PROJETO_MESA_ABANDONADO,
                "Projeto de mesa abandonado",
                "O projeto de mesa " + event.idProjetoMesa() + " foi marcado como abandonado. Ação tomada: "
                        + event.acaoTomada() + ".",
                event.idProjetoMesa());
    }

    @RabbitListener(queues = RabbitMqConfig.KANBAN_STATUS_QUEUE)
    public void onKanbanStatusAlterado(KanbanStatusAlteradoEvent event) {
        log.info("Evento recebido: kanban.status.alterado para a encomenda {}", event.idEncomenda());
        notificacaoService.registrar(null, CanalNotificacao.EMAIL, TipoEvento.ENCOMENDA_STATUS_ALTERADO,
                "Kanban atualizado",
                "A encomenda " + event.idEncomenda() + " mudou no Kanban de " + event.statusAnterior()
                        + " para " + event.statusNovo() + ".",
                event.idEncomenda());
    }

    @RabbitListener(queues = RabbitMqConfig.NIVEL_ALTERADO_QUEUE)
    public void onNivelAlterado(NivelAlteradoEvent event) {
        log.info("Evento recebido: nivel.alterado para o funcionário {}", event.idFuncionario());
        notificacaoService.registrar(event.idFuncionario(), CanalNotificacao.EMAIL, TipoEvento.NIVEL_ALTERADO,
                "Seu nível de acesso mudou",
                "Seu nível de acesso foi alterado de " + event.nivelAntigo() + " para "
                        + event.nivelNovo() + ".",
                event.idFuncionario());
    }

    @RabbitListener(queues = RabbitMqConfig.HORAS_VALIDADAS_QUEUE)
    public void onHorasValidadas(HorasValidadasEvent event) {
        log.info("Evento recebido: horas.validadas para o funcionário {}", event.idFuncionario());
        notificacaoService.registrar(event.idFuncionario(), CanalNotificacao.EMAIL, TipoEvento.HORAS_VALIDADAS,
                "Horas validadas",
                "Suas horas do tipo " + event.tipo() + " (" + event.horas()
                        + "h) em " + event.data() + " foram validadas.",
                event.idReferencia());
    }

    @RabbitListener(queues = RabbitMqConfig.CERTIFICADO_SOLICITADO_QUEUE)
    public void onCertificadoSolicitado(CertificadoSolicitadoEvent event) {
        log.info("Evento recebido: certificado.solicitado para a solicitação {}", event.idSolicitacao());
        notificacaoService.registrar(null, CanalNotificacao.EMAIL, TipoEvento.CERTIFICADO_SOLICITADO,
                "Nova solicitação de certificado",
                "O funcionário " + event.idFuncionario() + " solicitou um certificado do tipo "
                        + event.tipoCertificado() + " (" + event.horasSolicitadas() + "h) em "
                        + event.dataSolicitacao() + ".",
                event.idSolicitacao());
    }

    @RabbitListener(queues = RabbitMqConfig.CERTIFICADO_APROVADO_QUEUE)
    public void onCertificadoAprovado(CertificadoAprovadoEvent event) {
        log.info("Evento recebido: certificado.aprovado para o certificado {}", event.idCertificado());
        notificacaoService.registrar(event.idFuncionario(), CanalNotificacao.EMAIL,
                TipoEvento.CERTIFICADO_APROVADO,
                "Certificado de horas aprovado",
                "Olá " + event.nomeFuncionario() + ",\n"
                        + "Seu certificado do tipo " + event.tipoCertificado()
                        + " (" + event.horasCertificadas() + "h) foi aprovado e emitido em "
                        + event.dataEmissao() + ".",
                event.idCertificado());
    }

    @RabbitListener(queues = RabbitMqConfig.CERTIFICADO_REJEITADO_QUEUE)
    public void onCertificadoRejeitado(CertificadoRejeitadoEvent event) {
        log.info("Evento recebido: certificado.rejeitado para a solicitação {}", event.idSolicitacao());
        String motivo = event.observacao() == null ? "sem motivo informado" : event.observacao();
        notificacaoService.registrar(event.idFuncionario(), CanalNotificacao.EMAIL,
                TipoEvento.CERTIFICADO_REJEITADO,
                "Solicitação de certificado rejeitada",
                "Olá " + event.nomeFuncionario() + ",\n"
                        + "Sua solicitação de certificado do tipo " + event.tipoCertificado()
                        + " foi rejeitada (" + motivo + ").",
                event.idSolicitacao());
    }

    @RabbitListener(queues = RabbitMqConfig.EXTRATO_MENSAL_HORAS_QUEUE)
    public void onExtratoMensalHoras(ExtratoMensalHorasEvent event) {
        log.info("Evento recebido: extrato.mensal.horas para o funcionário {}", event.idFuncionario());
        notificacaoService.registrar(event.idFuncionario(), CanalNotificacao.EMAIL,
                TipoEvento.EXTRATO_MENSAL_HORAS,
                "Extrato mensal de horas",
                formatarExtratoMensal(event),
                event.idFuncionario());
    }

    /** Formata o corpo do e-mail com o resumo do extrato mensal. */
    public static String formatarExtratoMensal(ExtratoMensalHorasEvent event) {
        return "Olá " + event.nome() + ",\n"
                + "Segue o extrato de horas do mês " + event.mesReferencia() + ":\n"
                + "- Presença: " + event.horasPresenca() + "h\n"
                + "- Encomendas: " + event.horasEncomenda() + "h\n"
                + "- Projetos: " + event.horasProjeto() + "h\n"
                + "- Total disponível para certificado: " + event.horasDisponiveis() + "h";
    }
}
