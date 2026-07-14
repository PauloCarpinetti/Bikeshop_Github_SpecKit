package com.bikeshop.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findBySlugAndStatus(String slug, ProdutoStatus status);

    boolean existsBySlug(String slug);
}
