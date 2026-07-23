package com.portfolio_hub.subscription;

import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
  name = "workspace_subscriptions",
  uniqueConstraints = @UniqueConstraint(
    name = "uk_workspace_subscription",
    columnNames = { "workspaceType", "workspaceId" }
  ),
  indexes = {
    @Index(
      name = "idx_workspace_subscription_owner",
      columnList = "ownerId,workspaceType,status"
    ),
    @Index(
      name = "idx_workspace_subscription_plan",
      columnList = "planId,status"
    ),
  }
)
public class WorkspaceSubscription extends BaseEntity {

  public enum Status {
    PENDING_PAYMENT,
    ACTIVE,
    PAUSED,
    CANCELLED,
    EXPIRED,
  }

  @Column(nullable = false, columnDefinition = "TEXT")
  private String ownerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
  private WorkspaceType workspaceType;

  @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
  private String workspaceId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String planId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private Status status;

  private LocalDateTime startedAt;
  private LocalDateTime nextBillingAt;
  private boolean cancelAtPeriodEnd;
  private LocalDateTime cancelledAt;

  @Column(columnDefinition = "TEXT")
  private String externalCustomerId;

  @Column(columnDefinition = "TEXT")
  private String externalSubscriptionId;

  @Column(columnDefinition = "TEXT")
  private String adminNote;
}
