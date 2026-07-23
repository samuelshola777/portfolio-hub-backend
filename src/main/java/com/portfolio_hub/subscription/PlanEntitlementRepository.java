package com.portfolio_hub.subscription;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanEntitlementRepository
  extends JpaRepository<PlanEntitlement, String> {
  List<PlanEntitlement> findByPlanIdOrderByCodeAsc(String planId);

  List<PlanEntitlement> findByPlanIdIn(Collection<String> planIds);

  Optional<PlanEntitlement> findByPlanIdAndCode(
    String planId,
    EntitlementCode code
  );

  void deleteAllByPlanId(String planId);
}
