package com.shopmart.module.payment.service;

import com.shopmart.module.payment.dto.CreatePaymentRequest;
import com.shopmart.module.payment.dto.PaymentIntentResponse;
import com.shopmart.module.payment.dto.PaymentResponse;
import com.shopmart.module.payment.dto.RefundRequest;
import com.shopmart.module.payment.dto.VerifyPaymentRequest;
import com.shopmart.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentService {
    PaymentIntentResponse initiate(Long userId, CreatePaymentRequest request);
    PaymentResponse verify(Long userId, VerifyPaymentRequest request);
    List<PaymentResponse> history(Long userId, Long orderId);
    void handleWebhook(String payload, String razorpaySignature, String phonepeSignature, String stripeSignature);
    PaymentResponse refundOrder(Long orderId, RefundRequest request);
    PaymentResponse markFailed(Long userId, Long orderId, String reason);
    PageResponse<PaymentResponse> allPayments(Pageable pageable);
    java.util.List<com.shopmart.module.payment.entity.PaymentTransaction> transactions(Long orderId);
}
