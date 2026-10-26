package com.shopmart.module.payment.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.payment.dto.CreatePaymentRequest;
import com.shopmart.module.payment.dto.PaymentIntentResponse;
import com.shopmart.module.payment.dto.PaymentResponse;
import com.shopmart.module.payment.dto.VerifyPaymentRequest;
import com.shopmart.module.payment.service.PaymentService;
import com.shopmart.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments")
@lombok.extern.slf4j.Slf4j
public class PaymentController {

    private final PaymentService service;

    @PostMapping("/initiate")
    public ApiResponse<PaymentIntentResponse> initiate(@Valid @RequestBody CreatePaymentRequest request) {
        log.info("Create Payment Request: {}", request);
        log.info("Order Id: {}", request.orderId());
        try {
            return ApiResponse.ok("Payment initiated", service.initiate(SecurityUtils.currentUserId(), request));
        } catch (Exception e) {
            log.error("Razorpay Error", e);
            throw e;
        }
    }

    /** Alias for /initiate — creates a gateway order for the given order. */
    @PostMapping("/create-order")
    public ApiResponse<PaymentIntentResponse> createOrder(@Valid @RequestBody CreatePaymentRequest request) {
        log.info("Create Payment Request: {}", request);
        log.info("Order Id: {}", request.orderId());
        try {
            return ApiResponse.ok("Order created", service.initiate(SecurityUtils.currentUserId(), request));
        } catch (Exception e) {
            log.error("Razorpay Error", e);
            throw e;
        }
    }

    /**
     * Gateway webhook (Razorpay/PhonePe call this server-to-server).
     * Public endpoint; the gateway signature is verified inside the service.
     * Body and signature header are passed through for verification.
     */
    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(@RequestBody(required = false) String payload,
                                     @RequestHeader(value = "X-Razorpay-Signature", required = false) String razorpaySig,
                                     @RequestHeader(value = "X-Verify", required = false) String phonepeSig,
                                     @RequestHeader(value = "Stripe-Signature", required = false) String stripeSig) {
        service.handleWebhook(payload, razorpaySig, phonepeSig, stripeSig);
        return ApiResponse.message("ok");
    }

    @PostMapping("/verify")
    public ApiResponse<PaymentResponse> verify(@Valid @RequestBody VerifyPaymentRequest request) {
        log.info("Verify Payment Request: {}", request);
        log.info("Order Id: {}", request.orderId());
        try {
            return ApiResponse.ok("Payment verified", service.verify(SecurityUtils.currentUserId(), request));
        } catch (Exception e) {
            log.error("Razorpay Error", e);
            throw e;
        }
    }

    @PostMapping("/{orderId}/refund")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<PaymentResponse> refund(@PathVariable Long orderId,
                                               @RequestBody(required = false) com.shopmart.module.payment.dto.RefundRequest request) {
        return ApiResponse.ok("Refund processed", service.refundOrder(orderId, request));
    }

    @PostMapping("/{orderId}/fail")
    public ApiResponse<PaymentResponse> fail(@PathVariable Long orderId,
                                             @RequestParam(required = false) String reason) {
        return ApiResponse.ok("Payment marked failed",
                service.markFailed(SecurityUtils.currentUserId(), orderId, reason));
    }

    @GetMapping("/admin/all")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<com.shopmart.common.dto.PageResponse<PaymentResponse>> allPayments(
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ApiResponse.ok(service.allPayments(pageable));
    }

    @GetMapping("/{orderId}/transactions")
    public ApiResponse<java.util.List<com.shopmart.module.payment.entity.PaymentTransaction>> transactions(
            @PathVariable Long orderId) {
        return ApiResponse.ok(service.transactions(orderId));
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<List<PaymentResponse>> history(@PathVariable Long orderId) {
        return ApiResponse.ok(service.history(SecurityUtils.currentUserId(), orderId));
    }
}
