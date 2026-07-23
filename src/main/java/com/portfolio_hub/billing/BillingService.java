package com.portfolio_hub.billing;

import com.portfolio_hub.billing.BillingDtos.*;
import com.portfolio_hub.subscription.*;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.utils.exception.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingService {

  private final BillingPaymentRepository payments;
  private final WorkspaceSubscriptionRepository subscriptions;
  private final WorkspaceSubscriptionService subscriptionService;
  private final SubscriptionPlanService planService;
  private final UserRepository users;
  private final PaystackClient paystack;

  @Value("${application.billing.bank.bank-name:}")
  private String bankName;

  @Value("${application.billing.bank.account-name:}")
  private String bankAccountName;

  @Value("${application.billing.bank.account-number:}")
  private String bankAccountNumber;

  @Value(
    "${application.billing.bank.instructions:Use your payment reference as the transfer narration.}"
  )
  private String bankInstructions;

  @Value(
    "${application.billing.paystack.portfolio-callback-url:http://localhost:3000/billing/callback}"
  )
  private String portfolioCallbackUrl;

  @Value(
    "${application.billing.paystack.business-callback-url:http://localhost:3001/billing/callback}"
  )
  private String businessCallbackUrl;

  @Transactional
  public PaymentData checkout(CheckoutRequest request) {
    User user = currentUser();
    WorkspaceSubscription workspace = ownedWorkspace(
      user,
      request.workspaceType(),
      request.workspaceId()
    );
    SubscriptionPlan plan = planService.activePlan(
      request.planId(),
      request.workspaceType()
    );
    if (plan.isFree() || plan.getMonthlyPrice().signum() <= 0) {
      throw new InvalidInputException("Choose a paid subscription plan");
    }
    if (
      request.method() == PaymentMethod.BANK_TRANSFER &&
      (bankName.isBlank() ||
        bankAccountName.isBlank() ||
        bankAccountNumber.isBlank())
    ) {
      throw new InvalidOperationException(
        "Bank transfer is temporarily unavailable. Please choose Paystack or contact support."
      );
    }
    String reference =
      "BH-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    BillingPayment payment = payments.save(
      BillingPayment.builder()
        .reference(reference)
        .ownerId(user.getId())
        .workspaceType(request.workspaceType())
        .workspaceId(workspace.getWorkspaceId())
        .planId(plan.getId())
        .method(request.method())
        .status(PaymentStatus.INITIALIZED)
        .amount(plan.getMonthlyPrice())
        .currency(plan.getCurrency())
        .expiresAt(LocalDateTime.now().plusHours(48))
        .build()
    );
    if (request.method() == PaymentMethod.PAYSTACK) {
      PaystackClient.Initialization initialized = paystack.initialize(
        user.getEmailAddress(),
        payment.getAmount(),
        payment.getCurrency(),
        reference,
        request.workspaceType() == WorkspaceType.PORTFOLIO
          ? portfolioCallbackUrl
          : businessCallbackUrl
      );
      payment.setAuthorizationUrl(initialized.authorizationUrl());
      payment.setExternalReference(initialized.reference());
      payment = payments.save(payment);
    }
    return map(payment);
  }

  @Transactional
  public PaymentData submitTransferProof(
    String reference,
    TransferProofRequest request
  ) {
    User user = currentUser();
    BillingPayment payment = ownedPayment(reference, user);
    if (payment.getMethod() != PaymentMethod.BANK_TRANSFER) {
      throw new InvalidInputException(
        "This payment was not created for bank transfer"
      );
    }
    if (payment.getStatus() == PaymentStatus.PAID) return map(payment);
    if (payment.getExpiresAt().isBefore(LocalDateTime.now())) {
      payment.setStatus(PaymentStatus.EXPIRED);
      payments.save(payment);
      throw new InvalidOperationException(
        "This payment reference has expired. Start a new payment."
      );
    }
    payment.setTransferProofUrl(clean(request.proofUrl()));
    payment.setTransferSenderName(clean(request.senderName()));
    payment.setCustomerNote(clean(request.note()));
    payment.setStatus(PaymentStatus.PENDING_REVIEW);
    return map(payments.save(payment));
  }

  @Transactional
  public PaymentData verifyPaystack(String reference) {
    User user = currentUser();
    BillingPayment payment = ownedPayment(reference, user);
    return verifyPaystackPayment(payment);
  }

  @Transactional
  public void processPaystackWebhook(String reference) {
    if (reference == null || reference.isBlank()) return;
    payments
      .findByReferenceForUpdate(reference)
      .ifPresent(this::verifyPaystackPayment);
  }

  @Transactional(readOnly = true)
  public PaymentPage mine(int page, int size) {
    Page<BillingPayment> result = payments.findByOwnerIdOrderByCreatedAtDesc(
      currentUser().getId(),
      PageRequest.of(Math.max(0, page), clampSize(size))
    );
    return page(result);
  }

  @Transactional(readOnly = true)
  public PaymentPage adminPayments(PaymentStatus status, int page, int size) {
    PageRequest request = PageRequest.of(Math.max(0, page), clampSize(size));
    Page<BillingPayment> result = status == null
      ? payments.findAllByOrderByCreatedAtDesc(request)
      : payments.findByStatusOrderByCreatedAtAsc(status, request);
    return page(result);
  }

  @Transactional
  public PaymentData review(String reference, ReviewRequest request) {
    User admin = currentUser();
    BillingPayment payment = entity(reference);
    if (payment.getMethod() != PaymentMethod.BANK_TRANSFER) {
      throw new InvalidInputException(
        "Only bank transfers require manual review"
      );
    }
    if (payment.getStatus() == PaymentStatus.PAID) return map(payment);
    if (payment.getStatus() != PaymentStatus.PENDING_REVIEW) {
      throw new InvalidOperationException(
        "This transfer is not waiting for review"
      );
    }
    payment.setReviewedBy(admin.getId());
    payment.setReviewedAt(LocalDateTime.now());
    payment.setReviewNote(clean(request.note()));
    if (request.approve()) {
      markPaid(payment);
    } else {
      payment.setStatus(PaymentStatus.REJECTED);
      payments.save(payment);
    }
    return map(payment);
  }

  @Transactional
  public int expireAbandonedPayments() {
    var expired = payments.findByStatusAndExpiresAtBefore(
      PaymentStatus.INITIALIZED,
      LocalDateTime.now()
    );
    expired.forEach(payment -> payment.setStatus(PaymentStatus.EXPIRED));
    payments.saveAll(expired);
    return expired.size();
  }

  private PaymentData verifyPaystackPayment(BillingPayment payment) {
    if (payment.getMethod() != PaymentMethod.PAYSTACK) {
      throw new InvalidInputException("This payment is not a Paystack payment");
    }
    if (payment.getStatus() == PaymentStatus.PAID) return map(payment);
    PaystackClient.Verification verified = paystack.verify(
      payment.getReference()
    );
    if (
      !verified.successful() ||
      payment.getAmount().compareTo(verified.amount()) != 0 ||
      !payment.getCurrency().equalsIgnoreCase(verified.currency())
    ) {
      payment.setStatus(PaymentStatus.FAILED);
      payments.save(payment);
      throw new InvalidOperationException(
        "Payment was not completed successfully"
      );
    }
    payment.setExternalReference(verified.reference());
    markPaid(payment);
    return map(payment);
  }

  private void markPaid(BillingPayment payment) {
    if (payment.getStatus() == PaymentStatus.PAID) return;
    subscriptionService.activatePaidPlan(
      payment.getOwnerId(),
      payment.getWorkspaceType(),
      payment.getWorkspaceId(),
      payment.getPlanId()
    );
    payment.setStatus(PaymentStatus.PAID);
    payment.setPaidAt(LocalDateTime.now());
    payments.save(payment);
  }

  private WorkspaceSubscription ownedWorkspace(
    User user,
    WorkspaceType type,
    String id
  ) {
    WorkspaceSubscription workspace = subscriptions
      .findByWorkspaceTypeAndWorkspaceId(type, id)
      .orElseThrow(() ->
        new ResourceNotFoundException("Workspace subscription not found")
      );
    if (!workspace.getOwnerId().equals(user.getId())) {
      throw new ResourceNotFoundException("Workspace subscription not found");
    }
    return workspace;
  }

  private BillingPayment ownedPayment(String reference, User user) {
    BillingPayment payment = entity(reference);
    if (!payment.getOwnerId().equals(user.getId())) {
      throw new ResourceNotFoundException("Payment not found");
    }
    return payment;
  }

  private BillingPayment entity(String reference) {
    return payments
      .findByReferenceForUpdate(reference)
      .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
  }

  private PaymentPage page(Page<BillingPayment> result) {
    return new PaymentPage(
      result.getContent().stream().map(this::map).toList(),
      result.getNumber(),
      result.getSize(),
      result.getTotalElements(),
      result.getTotalPages()
    );
  }

  private PaymentData map(BillingPayment payment) {
    var plan = planService.get(payment.getPlanId());
    return new PaymentData(
      payment.getId(),
      payment.getReference(),
      payment.getWorkspaceType(),
      payment.getWorkspaceId(),
      payment.getMethod(),
      payment.getStatus(),
      payment.getAmount(),
      payment.getCurrency(),
      payment.getAuthorizationUrl(),
      payment.getTransferProofUrl(),
      payment.getTransferSenderName(),
      payment.getCustomerNote(),
      payment.getReviewNote(),
      payment.getPaidAt(),
      payment.getExpiresAt(),
      payment.getCreatedAt(),
      plan,
      payment.getMethod() == PaymentMethod.BANK_TRANSFER ? bankAccount() : null
    );
  }

  private BankAccountData bankAccount() {
    return new BankAccountData(
      bankName,
      bankAccountName,
      bankAccountNumber,
      bankInstructions
    );
  }

  private User currentUser() {
    String email = SecurityContextHolder.getContext()
      .getAuthentication()
      .getName();
    return users
      .findByEmailAddressIgnoreCaseAndDeletedFalse(email)
      .orElseThrow(() ->
        new UnauthorizedException("Please sign in to continue")
      );
  }

  private int clampSize(int size) {
    return Math.min(100, Math.max(1, size));
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
