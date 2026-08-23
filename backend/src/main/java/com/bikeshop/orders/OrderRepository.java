package com.bikeshop.orders;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByPaymentReference(String paymentReference);

    List<Pedido> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

    Optional<Pedido> findByIdAndClienteId(Long id, Long clienteId);
}
