package com.shopmart.module.payment.repository;

import com.shopmart.module.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByGatewayRef(String gatewayRef);

    /** Payments stuck in PENDING past the reconciliation window — candidates for a gateway status check. */
    List<Payment> findByStatusAndCreatedAtBefore(Payment.PaymentState status, java.time.Instant before);
}
