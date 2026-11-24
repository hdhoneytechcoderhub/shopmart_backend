package com.shopmart.module.payment.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.payment.dto.CreatePaymentRequest;
import com.shopmart.module.payment.dto.OrderPaymentStatusResponse;
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
        return ApiResponse.ok("Payment initiated", service.initiate(SecurityUtils.currentUserId(), request));
    }

    /** Alias for /initiate — creates a gateway order for the given order. */
    @PostMapping("/create-order")
    public ApiResponse<PaymentIntentResponse> createOrder(@Valid @RequestBody CreatePaymentRequest request) {
        return ApiResponse.ok("Order created", service.initiate(SecurityUtils.currentUserId(), request));
    }

    /**
     * Gateway webhook (Razorpay calls this server-to-server). Public endpoint;
     * the raw body signature is verified inside the service before anything is applied.
     */
    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(@RequestBody(required = false) String payload,
                                     @RequestHeader(value = "X-Razorpay-Signature", required = false) String razorpaySig,
                                     @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String razorpayEventId,
                                     @RequestHeader(value = "X-Verify", required = false) String phonepeSig,
                                     @RequestHeader(value = "Stripe-Signature", required = false) String stripeSig) {
        service.handleWebhook(payload, razorpayEventId, razorpaySig, phonepeSig, stripeSig);
        return ApiResponse.message("ok");
    }

    @PostMapping("/verify")
    public ApiResponse<PaymentResponse> verify(@Valid @RequestBody VerifyPaymentRequest request) {
        return ApiResponse.ok("Payment verified", service.verify(SecurityUtils.currentUserId(), request));
    }

    /** Re-reads the gateway's authoritative state and settles the order if it was paid. */
    @PostMapping("/order/{orderId}/sync")
    public ApiResponse<OrderPaymentStatusResponse> sync(@PathVariable Long orderId) {
        return ApiResponse.ok(service.syncStatus(SecurityUtils.currentUserId(), orderId));
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
    public ApiResponse<List<com.shopmart.module.payment.entity.PaymentTransaction>> transactions(
            @PathVariable Long orderId) {
        return ApiResponse.ok(service.transactions(SecurityUtils.currentUserId(), orderId));
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<List<PaymentResponse>> history(@PathVariable Long orderId) {
        return ApiResponse.ok(service.history(SecurityUtils.currentUserId(), orderId));
    }
}
