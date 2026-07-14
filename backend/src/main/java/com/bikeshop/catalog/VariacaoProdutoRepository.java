package com.bikeshop.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariacaoProdutoRepository extends JpaRepository<VariacaoProduto, Long> {

    List<VariacaoProduto> findByProdutoIdOrderByIdAsc(Long produtoId);

    Optional<VariacaoProduto> findBySku(String sku);
}
