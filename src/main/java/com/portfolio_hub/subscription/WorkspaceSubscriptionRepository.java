package com.portfolio_hub.subscription;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceSubscriptionRepository
  extends JpaRepository<WorkspaceSubscription, String> {
  Optional<WorkspaceSubscription> findByWorkspaceTypeAndWorkspaceId(
    WorkspaceType workspaceType,
    String workspaceId
  );

  List<WorkspaceSubscription> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

  @Query(
    "select count(s) from WorkspaceSubscription s, SubscriptionPlan p " +
      "where s.planId = p.id and s.ownerId = :ownerId " +
      "and s.workspaceType = :workspaceType and p.free = true " +
      "and s.status in :statuses"
  )
  long countFreeSubscriptions(
    @Param("ownerId") String ownerId,
    @Param("workspaceType") WorkspaceType workspaceType,
    @Param("statuses") List<WorkspaceSubscription.Status> statuses
  );

  long countByPlanId(String planId);

  List<WorkspaceSubscription> findByStatusAndNextBillingAtBefore(
    WorkspaceSubscription.Status status,
    java.time.LocalDateTime nextBillingAt
  );

  void deleteAllByOwnerId(String ownerId);
}
