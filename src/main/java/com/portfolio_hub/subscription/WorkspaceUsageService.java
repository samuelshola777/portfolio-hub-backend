package com.portfolio_hub.subscription;

import com.portfolio_hub.business.BusinessItem;
import com.portfolio_hub.business.BusinessItemRepository;
import com.portfolio_hub.subscription.SubscriptionDtos.UsageData;
import com.portfolio_hub.subscription.SubscriptionDtos.UsageItem;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceUsageService {

  private final WorkspaceSubscriptionService subscriptionService;
  private final SubscriptionPlanRepository plans;
  private final PlanEntitlementRepository entitlements;
  private final BusinessItemRepository businessItems;

  @Transactional(readOnly = true)
  public UsageData usage(WorkspaceType type, String workspaceId) {
    WorkspaceSubscription subscription = subscriptionService.requireActive(
      type,
      workspaceId
    );
    SubscriptionPlan plan = plans
      .findById(subscription.getPlanId())
      .orElseThrow(() ->
        new ResourceNotFoundException("Subscription plan not found")
      );
    Map<EntitlementCode, Long> counts = type == WorkspaceType.BUSINESS
      ? businessUsage(workspaceId)
      : portfolioUsage();
    List<UsageItem> values = entitlements
      .findByPlanIdOrderByCodeAsc(plan.getId())
      .stream()
      .map(value -> map(value, counts.getOrDefault(value.getCode(), 0L)))
      .toList();
    return new UsageData(type, workspaceId, plan.getName(), values);
  }

  private Map<EntitlementCode, Long> businessUsage(String businessId) {
    Map<EntitlementCode, Long> usage = new EnumMap<>(EntitlementCode.class);
    usage.put(EntitlementCode.PAGES, count(businessId, BusinessItem.Type.PAGE));
    usage.put(
      EntitlementCode.SECTIONS,
      count(businessId, BusinessItem.Type.SECTION)
    );
    usage.put(
      EntitlementCode.PRODUCTS,
      count(businessId, BusinessItem.Type.PRODUCT)
    );
    usage.put(
      EntitlementCode.MUSIC_TRACKS,
      count(businessId, BusinessItem.Type.MUSIC_TRACK)
    );
    return usage;
  }

  private Map<EntitlementCode, Long> portfolioUsage() {
    Map<EntitlementCode, Long> usage = new EnumMap<>(EntitlementCode.class);
    usage.put(EntitlementCode.PAGES, 1L);
    return usage;
  }

  private long count(String businessId, BusinessItem.Type type) {
    return businessItems.countByBusinessIdAndTypeAndDeletedFalse(
      businessId,
      type
    );
  }

  private UsageItem map(PlanEntitlement value, long used) {
    if (value.getValueType() == EntitlementValueType.INTEGER) {
      long limit = Long.parseLong(value.getValue());
      return new UsageItem(
        value.getCode(),
        value.getCode().displayName(),
        value.getValueType(),
        used,
        limit,
        null,
        limit < 0
      );
    }
    if (value.getValueType() == EntitlementValueType.BOOLEAN) {
      return new UsageItem(
        value.getCode(),
        value.getCode().displayName(),
        value.getValueType(),
        null,
        null,
        Boolean.parseBoolean(value.getValue()),
        false
      );
    }
    return new UsageItem(
      value.getCode(),
      value.getCode().displayName(),
      value.getValueType(),
      null,
      null,
      null,
      false
    );
  }
}
