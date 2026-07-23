package com.portfolio_hub.billing;

import com.portfolio_hub.subscription.WorkspaceType;
import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
  name = "billing_payments",
  indexes = {
    @Index(
      name = "idx_billing_reference",
      columnList = "reference",
      unique = true
    ),
    @Index(name = "idx_billing_owner", columnList = "ownerId,createdAt"),
    @Index(name = "idx_billing_review", columnList = "status,createdAt"),
  }
)
public class BillingPayment extends BaseEntity {

  @Column(
    nullable = false,
    unique = true,
    updatable = false,
    columnDefinition = "TEXT"
  )
  private String reference;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String ownerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private WorkspaceType workspaceType;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String workspaceId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String planId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private PaymentMethod method;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private PaymentStatus status;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String currency;

  @Column(columnDefinition = "TEXT")
  private String authorizationUrl;

  @Column(columnDefinition = "TEXT")
  private String externalReference;

  @Column(columnDefinition = "TEXT")
  private String transferProofUrl;

  @Column(columnDefinition = "TEXT")
  private String transferSenderName;

  @Column(columnDefinition = "TEXT")
  private String customerNote;

  @Column(columnDefinition = "TEXT")
  private String reviewNote;

  @Column(columnDefinition = "TEXT")
  private String reviewedBy;

  private LocalDateTime paidAt;
  private LocalDateTime reviewedAt;
  private LocalDateTime expiresAt;
}
