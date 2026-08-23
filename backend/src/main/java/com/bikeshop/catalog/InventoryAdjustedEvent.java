package com.bikeshop.catalog;

/**
 * Evento publicado em {@code inventory.events} (routing key {@code inventory.adjusted}) sempre que
 * o estoque de uma variação é debitado (checkout) ou ajustado (backoffice, Fase 5).
 */
public record InventoryAdjustedEvent(
        Long variacaoProdutoId,
        String sku,
        int quantidadeDebitada,
        int estoqueResultante
) {
}
