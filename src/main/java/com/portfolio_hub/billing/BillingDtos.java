package com.portfolio_hub.billing;

import com.portfolio_hub.subscription.SubscriptionDtos.PlanData;
import com.portfolio_hub.subscription.WorkspaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class BillingDtos {

  private BillingDtos() {}

  public record CheckoutRequest(
    @NotNull WorkspaceType workspaceType,
    @NotBlank String workspaceId,
    @NotBlank String planId,
    @NotNull PaymentMethod method
  ) {}

  public record TransferProofRequest(
    @NotBlank String proofUrl,
    @NotBlank String senderName,
    String note
  ) {}

  public record ReviewRequest(boolean approve, String note) {}

  public record BankAccountData(
    String bankName,
    String accountName,
    String accountNumber,
    String instructions
  ) {}

  public record PaymentData(
    String id,
    String reference,
    WorkspaceType workspaceType,
    String workspaceId,
    PaymentMethod method,
    PaymentStatus status,
    BigDecimal amount,
    String currency,
    String authorizationUrl,
    String transferProofUrl,
    String transferSenderName,
    String customerNote,
    String reviewNote,
    LocalDateTime paidAt,
    LocalDateTime expiresAt,
    LocalDateTime createdAt,
    PlanData plan,
    BankAccountData bankAccount
  ) {}

  public record PaymentPage(
    List<PaymentData> items,
    int page,
    int size,
    long totalItems,
    int totalPages
  ) {}
}
