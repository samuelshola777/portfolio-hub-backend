package com.portfolio_hub.billing;

import com.portfolio_hub.billing.BillingDtos.*;
import com.portfolio_hub.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/billing")
@RequiredArgsConstructor
public class AdminBillingController {

  private final BillingService billing;

  @GetMapping("/payments")
  public ResponseEntity<ApiResponse<PaymentPage>> payments(
    @RequestParam(required = false) PaymentStatus status,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Payments fetched",
        billing.adminPayments(status, page, size)
      )
    );
  }

  @PutMapping("/payments/{reference}/review")
  public ResponseEntity<ApiResponse<PaymentData>> review(
    @PathVariable String reference,
    @RequestBody ReviewRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        request.approve()
          ? "Transfer approved and subscription activated"
          : "Transfer rejected",
        billing.review(reference, request)
      )
    );
  }
}
