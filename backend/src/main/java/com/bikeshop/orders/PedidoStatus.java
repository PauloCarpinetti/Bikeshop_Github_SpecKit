package com.bikeshop.orders;

/**
 * Ciclo de vida do pedido (FR-007, data-model.md). Transições são controladas por
 * {@link OrderService#atualizarStatus} — não é permitido "voltar" de um estado terminal.
 */
public enum PedidoStatus {
    CRIADO,
    AGUARDANDO_PAGAMENTO,
    PAGO,
    EM_SEPARACAO,
    ENVIADO,
    ENTREGUE,
    PAGAMENTO_RECUSADO,
    CANCELADO,
    EM_TROCA_DEVOLUCAO
}
