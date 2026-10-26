package com.shopmart.module.payment.repository;

import com.shopmart.module.payment.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByOrderIdOrderByCreatedAtAsc(Long orderId);
    Page<PaymentTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
    java.util.Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
}
