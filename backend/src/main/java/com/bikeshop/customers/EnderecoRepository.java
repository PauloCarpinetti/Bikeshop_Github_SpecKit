package com.bikeshop.customers;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {

    List<Endereco> findByClienteIdOrderByPadraoDescIdAsc(Long clienteId);

    Optional<Endereco> findByIdAndClienteId(Long id, Long clienteId);
}
