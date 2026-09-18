package com.fablab.notification.entity;

/**
 * Tipos de evento que originam notificações, padronizando o campo
 * {@code tipoEvento} consumido pelos endpoints.
 */
public enum TipoEvento {
    ESTOQUE_BAIXO,
    EMPRESTIMO_ATRASADO,
    ENCOMENDA_CRIADA,
    ENCOMENDA_STATUS_ALTERADO,
    ORCAMENTO_APROVADO,
    LANCAMENTO_VENCIDO,
    ADVERTENCIA_REGISTRADA,
    PROJETO_MESA_ABANDONADO,
    NIVEL_ALTERADO,
    HORAS_VALIDADAS,
    COMPRA_SOLICITADA,
    CERTIFICADO_SOLICITADO,
    CERTIFICADO_APROVADO,
    CERTIFICADO_REJEITADO,
    EXTRATO_MENSAL_HORAS,
    TESTE
}
