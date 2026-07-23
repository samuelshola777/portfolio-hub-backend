package com.portfolio_hub.billing;

import com.portfolio_hub.billing.BillingDtos.*;
import com.portfolio_hub.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

  private final BillingService billing;
  private final PaystackClient paystack;

  @PostMapping("/private/checkout")
  public ResponseEntity<ApiResponse<PaymentData>> checkout(
    @Valid @RequestBody CheckoutRequest request
  ) {
    return ResponseEntity.status(201).body(
      ApiResponse.success("Payment created", billing.checkout(request))
    );
  }

  @PutMapping("/private/payments/{reference}/transfer-proof")
  public ResponseEntity<ApiResponse<PaymentData>> transferProof(
    @PathVariable String reference,
    @Valid @RequestBody TransferProofRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Transfer submitted for review",
        billing.submitTransferProof(reference, request)
      )
    );
  }

  @PostMapping("/private/payments/{reference}/verify")
  public ResponseEntity<ApiResponse<PaymentData>> verify(
    @PathVariable String reference
  ) {
    return ResponseEntity.ok(
      ApiResponse.success("Payment verified", billing.verifyPaystack(reference))
    );
  }

  @GetMapping("/private/payments")
  public ResponseEntity<ApiResponse<PaymentPage>> mine(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success("Payment history fetched", billing.mine(page, size))
    );
  }

  @PostMapping("/public/paystack/webhook")
  public ResponseEntity<Void> webhook(
    @RequestHeader(
      value = "x-paystack-signature",
      required = false
    ) String signature,
    @RequestBody String payload
  ) {
    if (
      !paystack.validSignature(payload, signature)
    ) return ResponseEntity.status(401).build();
    billing.processPaystackWebhook(paystack.webhookReference(payload));
    return ResponseEntity.ok().build();
  }
}
