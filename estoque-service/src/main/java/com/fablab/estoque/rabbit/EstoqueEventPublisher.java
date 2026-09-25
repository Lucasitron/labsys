package com.fablab.estoque.rabbit;

import com.fablab.estoque.config.RabbitConfig;
import com.fablab.estoque.dto.CompraSolicitadaEvent;
import com.fablab.estoque.dto.EmprestimoAtrasadoEvent;
import com.fablab.estoque.dto.EstoqueBaixoEvent;
import com.fablab.estoque.entity.Emprestimo;
import com.fablab.estoque.entity.EntradaEstoque;
import com.fablab.estoque.entity.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica os eventos de domínio do Estoque no RabbitMQ:
 * {@code compra.solicitada.event}, {@code estoque.baixo.event} e
 * {@code emprestimo.atrasado.event}.
 */
@Service
public class EstoqueEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EstoqueEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public EstoqueEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Publica {@code compra.solicitada.event} para o Financeiro e o Notification. */
    public void publishCompraSolicitada(EntradaEstoque entrada) {
        CompraSolicitadaEvent event = new CompraSolicitadaEvent(
                entrada.getId(),
                entrada.getItem().getId(),
                entrada.getFornecedor().getId(),
                entrada.getQuantidade(),
                entrada.getValorUnitario(),
                entrada.getValorTotal(),
                entrada.getDataEntrada(),
                entrada.getNotaFiscal());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.ESTOQUE_EXCHANGE, RabbitConfig.COMPRA_SOLICITADA_ROUTING_KEY, event);
            log.info("Evento compra.solicitada.event publicado para a entrada {}", entrada.getId());
        } catch (RuntimeException ex) {
            log.warn("Falha ao publicar compra.solicitada.event para a entrada {}: {}",
                    entrada.getId(), ex.getMessage());
        }
    }

    /** Publica {@code estoque.baixo.event} quando o item fica igual ou abaixo do mínimo. */
    public void publishEstoqueBaixo(Item item) {
        EstoqueBaixoEvent event = new EstoqueBaixoEvent(
                item.getId(), item.getNome(), item.getQuantidadeAtual(), item.getEstoqueMinimo());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.ESTOQUE_EXCHANGE, RabbitConfig.ESTOQUE_BAIXO_ROUTING_KEY, event);
            log.info("Evento estoque.baixo.event publicado para o item {} ({})", item.getId(), item.getNome());
        } catch (RuntimeException ex) {
            log.warn("Falha ao publicar estoque.baixo.event para o item {}: {}", item.getId(), ex.getMessage());
        }
    }

    /** Publica {@code emprestimo.atrasado.event} para empréstimos vencidos. */
    public void publishEmprestimoAtrasado(Emprestimo emprestimo) {
        EmprestimoAtrasadoEvent event = new EmprestimoAtrasadoEvent(
                emprestimo.getId(),
                emprestimo.getIdPessoa(),
                emprestimo.getItem().getId(),
                emprestimo.getDataDevolucaoPrevista());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.ESTOQUE_EXCHANGE, RabbitConfig.EMPRESTIMO_ATRASADO_ROUTING_KEY, event);
            log.info("Evento emprestimo.atrasado.event publicado para o empréstimo {}", emprestimo.getId());
        } catch (RuntimeException ex) {
            log.warn("Falha ao publicar emprestimo.atrasado.event para o empréstimo {}: {}",
                    emprestimo.getId(), ex.getMessage());
        }
    }
}