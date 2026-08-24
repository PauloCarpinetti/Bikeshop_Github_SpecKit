package com.bikeshop.admin;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CupomDescontoRepository extends JpaRepository<CupomDesconto, Long> {

    Optional<CupomDesconto> findByCodigoIgnoreCase(String codigo);
}
