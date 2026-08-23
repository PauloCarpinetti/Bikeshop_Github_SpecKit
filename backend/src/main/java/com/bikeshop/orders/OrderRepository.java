package com.bikeshop.orders;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByPaymentReference(String paymentReference);
}
