package com.portfolio_hub.subscription;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository
  extends JpaRepository<SubscriptionPlan, String> {
  Optional<SubscriptionPlan> findByCodeIgnoreCase(String code);

  Optional<
    SubscriptionPlan
  > findFirstByWorkspaceTypeAndFreeTrueAndActiveTrueOrderBySortOrderAsc(
    WorkspaceType workspaceType
  );

  List<
    SubscriptionPlan
  > findByWorkspaceTypeAndActiveTrueAndPublicVisibleTrueOrderBySortOrderAsc(
    WorkspaceType workspaceType
  );

  List<SubscriptionPlan> findAllByOrderByWorkspaceTypeAscSortOrderAsc();
}
