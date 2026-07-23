package com.portfolio_hub.subscription;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class SubscriptionDtos {

  private SubscriptionDtos() {}

  public record EntitlementRequest(
    @NotNull EntitlementCode code,
    @NotBlank String value
  ) {}

  public record PlanRequest(
    String code,
    @NotBlank String name,
    String description,
    @NotNull WorkspaceType workspaceType,
    @NotNull @DecimalMin("0.00") BigDecimal monthlyPrice,
    String currency,
    Boolean free,
    Boolean active,
    Boolean publicVisible,
    Integer sortOrder,
    @NotEmpty List<@Valid EntitlementRequest> entitlements
  ) {}

  public record EntitlementData(
    EntitlementCode code,
    String name,
    EntitlementValueType valueType,
    String value
  ) {}

  public record PlanData(
    String id,
    String code,
    String name,
    String description,
    WorkspaceType workspaceType,
    BigDecimal monthlyPrice,
    String currency,
    boolean free,
    boolean active,
    boolean publicVisible,
    int sortOrder,
    List<EntitlementData> entitlements
  ) {}

  public record SubscriptionData(
    String id,
    String ownerId,
    WorkspaceType workspaceType,
    String workspaceId,
    WorkspaceSubscription.Status status,
    LocalDateTime startedAt,
    LocalDateTime nextBillingAt,
    boolean cancelAtPeriodEnd,
    LocalDateTime cancelledAt,
    PlanData plan
  ) {}

  public record UsageData(
    WorkspaceType workspaceType,
    String workspaceId,
    String planName,
    List<UsageItem> usage
  ) {}

  public record UsageItem(
    EntitlementCode code,
    String name,
    EntitlementValueType valueType,
    Long used,
    Long limit,
    Boolean enabled,
    boolean unlimited
  ) {}

  public record AssignmentRequest(
    @NotNull WorkspaceType workspaceType,
    @NotBlank String workspaceId,
    @NotBlank String planId,
    WorkspaceSubscription.Status status,
    String adminNote
  ) {}
}
