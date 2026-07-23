package com.portfolio_hub.subscription;

import com.portfolio_hub.utils.exception.InvalidOperationException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EntitlementService {

  private final WorkspaceSubscriptionService subscriptionService;
  private final PlanEntitlementRepository entitlements;

  @Transactional(readOnly = true)
  public void requireFeature(
    WorkspaceType workspaceType,
    String workspaceId,
    EntitlementCode code
  ) {
    WorkspaceSubscription subscription = subscriptionService.requireActive(
      workspaceType,
      workspaceId
    );
    PlanEntitlement entitlement = entitlement(
      subscription.getPlanId(),
      code
    ).orElseThrow(() -> unavailable(code));
    if (
      entitlement.getValueType() != EntitlementValueType.BOOLEAN ||
      !Boolean.parseBoolean(entitlement.getValue())
    ) {
      throw unavailable(code);
    }
  }

  @Transactional(readOnly = true)
  public void requireCapacity(
    WorkspaceType workspaceType,
    String workspaceId,
    EntitlementCode code,
    long currentUsage,
    long amountToAdd
  ) {
    WorkspaceSubscription subscription = subscriptionService.requireActive(
      workspaceType,
      workspaceId
    );
    PlanEntitlement entitlement = entitlement(
      subscription.getPlanId(),
      code
    ).orElseThrow(() -> unavailable(code));
    if (entitlement.getValueType() != EntitlementValueType.INTEGER) {
      throw unavailable(code);
    }
    long limit = Long.parseLong(entitlement.getValue());
    if (limit >= 0 && currentUsage + Math.max(0, amountToAdd) > limit) {
      throw new InvalidOperationException(
        "Your current plan allows " +
          limit +
          " " +
          code.displayName().toLowerCase() +
          ". Upgrade the plan to add more."
      );
    }
  }

  @Transactional(readOnly = true)
  public long limit(
    WorkspaceType workspaceType,
    String workspaceId,
    EntitlementCode code
  ) {
    WorkspaceSubscription subscription = subscriptionService.requireActive(
      workspaceType,
      workspaceId
    );
    return entitlement(subscription.getPlanId(), code)
      .filter(value -> value.getValueType() == EntitlementValueType.INTEGER)
      .map(value -> Long.parseLong(value.getValue()))
      .orElse(0L);
  }

  private Optional<PlanEntitlement> entitlement(
    String planId,
    EntitlementCode code
  ) {
    return entitlements.findByPlanIdAndCode(planId, code);
  }

  private InvalidOperationException unavailable(EntitlementCode code) {
    return new InvalidOperationException(
      code.displayName() + " is not included in the current plan"
    );
  }
}
