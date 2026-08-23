package com.bikeshop.reviews;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    boolean existsByClienteIdAndProdutoIdAndPedidoId(Long clienteId, Long produtoId, Long pedidoId);
}
