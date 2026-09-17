package com.fablab.notification.messaging;

import com.fablab.notification.config.RabbitMqConfig;
import com.fablab.notification.dto.AdvertenciaRegistradaEvent;
import com.fablab.notification.dto.CompraSolicitadaEvent;
import com.fablab.notification.dto.EmprestimoAtrasadoEvent;
import com.fablab.notification.dto.EncomendaCriadaEvent;
import com.fablab.notification.dto.EncomendaStatusAlteradoEvent;
import com.fablab.notification.dto.EstoqueBaixoEvent;
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
}
