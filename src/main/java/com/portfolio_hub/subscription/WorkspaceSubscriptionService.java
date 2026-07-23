package com.portfolio_hub.subscription;

import com.portfolio_hub.subscription.SubscriptionDtos.SubscriptionData;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.utils.exception.InvalidOperationException;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import com.portfolio_hub.utils.exception.UnauthorizedException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceSubscriptionService {

  private static final List<
    WorkspaceSubscription.Status
  > RESERVED_FREE_STATUSES = List.of(
    WorkspaceSubscription.Status.ACTIVE,
    WorkspaceSubscription.Status.PAUSED
  );

  private final WorkspaceSubscriptionRepository subscriptions;
  private final SubscriptionPlanRepository plans;
  private final SubscriptionPlanService planService;
  private final UserRepository users;

  @Transactional
  public WorkspaceSubscription provision(
    String ownerId,
    WorkspaceType workspaceType,
    String workspaceId,
    String requestedPlanId
  ) {
    return subscriptions
      .findByWorkspaceTypeAndWorkspaceId(workspaceType, workspaceId)
      .orElseGet(() -> {
        SubscriptionPlan plan = requestedPlanId == null ||
          requestedPlanId.isBlank()
          ? planService.defaultFreePlan(workspaceType)
          : planService.activePlan(requestedPlanId, workspaceType);
        if (plan.isFree() && hasFreeWorkspace(ownerId, workspaceType)) {
          throw new InvalidOperationException(
            "Your free plan already covers one " +
              workspaceType.name().toLowerCase() +
              ". Choose a paid plan to create another one."
          );
        }
        WorkspaceSubscription.Status status = plan.isFree()
          ? WorkspaceSubscription.Status.ACTIVE
          : WorkspaceSubscription.Status.PENDING_PAYMENT;
        return subscriptions.save(
          WorkspaceSubscription.builder()
            .ownerId(ownerId)
            .workspaceType(workspaceType)
            .workspaceId(workspaceId)
            .planId(plan.getId())
            .status(status)
            .startedAt(
              status == WorkspaceSubscription.Status.ACTIVE
                ? LocalDateTime.now()
                : null
            )
            .build()
        );
      });
  }

  @Transactional
  public WorkspaceSubscription provisionLegacyFree(
    String ownerId,
    WorkspaceType workspaceType,
    String workspaceId
  ) {
    return subscriptions
      .findByWorkspaceTypeAndWorkspaceId(workspaceType, workspaceId)
      .orElseGet(() -> {
        SubscriptionPlan plan = planService.defaultFreePlan(workspaceType);
        return subscriptions.save(
          WorkspaceSubscription.builder()
            .ownerId(ownerId)
            .workspaceType(workspaceType)
            .workspaceId(workspaceId)
            .planId(plan.getId())
            .status(WorkspaceSubscription.Status.ACTIVE)
            .startedAt(LocalDateTime.now())
            .adminNote("Automatically protected as an existing workspace")
            .build()
        );
      });
  }

  @Transactional
  public SubscriptionData assign(
    String ownerId,
    WorkspaceType workspaceType,
    String workspaceId,
    String planId,
    WorkspaceSubscription.Status status,
    String adminNote
  ) {
    SubscriptionPlan plan = planService.activePlan(planId, workspaceType);
    WorkspaceSubscription subscription = subscriptions
      .findByWorkspaceTypeAndWorkspaceId(workspaceType, workspaceId)
      .orElseGet(() ->
        WorkspaceSubscription.builder()
          .ownerId(ownerId)
          .workspaceType(workspaceType)
          .workspaceId(workspaceId)
          .build()
      );
    subscription.setOwnerId(ownerId);
    subscription.setPlanId(plan.getId());
    subscription.setStatus(
      status == null ? WorkspaceSubscription.Status.ACTIVE : status
    );
    subscription.setAdminNote(clean(adminNote));
    if (
      subscription.getStatus() == WorkspaceSubscription.Status.ACTIVE &&
      subscription.getStartedAt() == null
    ) {
      subscription.setStartedAt(LocalDateTime.now());
    }
    return map(subscriptions.save(subscription));
  }

  @Transactional(readOnly = true)
  public List<SubscriptionData> mine() {
    return subscriptions
      .findByOwnerIdOrderByCreatedAtDesc(currentUser().getId())
      .stream()
      .map(this::map)
      .toList();
  }

  @Transactional(readOnly = true)
  public SubscriptionData mine(WorkspaceType type, String workspaceId) {
    User user = currentUser();
    WorkspaceSubscription subscription = entity(type, workspaceId);
    if (!subscription.getOwnerId().equals(user.getId())) {
      throw new ResourceNotFoundException("Subscription not found");
    }
    return map(subscription);
  }

  @Transactional(readOnly = true)
  public WorkspaceSubscription requireActive(
    WorkspaceType type,
    String workspaceId
  ) {
    WorkspaceSubscription subscription = entity(type, workspaceId);
    if (
      subscription.getStatus() != WorkspaceSubscription.Status.ACTIVE ||
      (subscription.getNextBillingAt() != null &&
        !subscription.getNextBillingAt().isAfter(LocalDateTime.now()))
    ) {
      throw new InvalidOperationException(
        "Choose or activate a subscription plan to continue"
      );
    }
    return subscription;
  }

  @Transactional(readOnly = true)
  public SubscriptionData data(WorkspaceType type, String workspaceId) {
    return map(entity(type, workspaceId));
  }

  @Transactional
  public WorkspaceSubscription activatePaidPlan(
    String ownerId,
    WorkspaceType type,
    String workspaceId,
    String planId
  ) {
    SubscriptionPlan plan = planService.activePlan(planId, type);
    if (plan.isFree()) {
      throw new InvalidOperationException("Choose a paid subscription plan");
    }
    WorkspaceSubscription subscription = entity(type, workspaceId);
    if (!subscription.getOwnerId().equals(ownerId)) {
      throw new ResourceNotFoundException("Subscription not found");
    }
    LocalDateTime now = LocalDateTime.now();
    boolean sameActivePlan =
      subscription.getStatus() == WorkspaceSubscription.Status.ACTIVE &&
      subscription.getPlanId().equals(planId) &&
      subscription.getNextBillingAt() != null &&
      subscription.getNextBillingAt().isAfter(now);
    LocalDateTime periodStart = sameActivePlan
      ? subscription.getNextBillingAt()
      : now;
    subscription.setPlanId(planId);
    subscription.setStatus(WorkspaceSubscription.Status.ACTIVE);
    if (subscription.getStartedAt() == null) subscription.setStartedAt(now);
    subscription.setNextBillingAt(periodStart.plusMonths(1));
    subscription.setCancelAtPeriodEnd(false);
    subscription.setCancelledAt(null);
    return subscriptions.save(subscription);
  }

  @Transactional
  public SubscriptionData scheduleCancellation(
    WorkspaceType type,
    String workspaceId
  ) {
    User user = currentUser();
    WorkspaceSubscription subscription = entity(type, workspaceId);
    if (!subscription.getOwnerId().equals(user.getId())) {
      throw new ResourceNotFoundException("Subscription not found");
    }
    if (subscription.getStatus() != WorkspaceSubscription.Status.ACTIVE) {
      throw new InvalidOperationException("This subscription is not active");
    }
    subscription.setCancelAtPeriodEnd(true);
    subscription.setCancelledAt(LocalDateTime.now());
    return map(subscriptions.save(subscription));
  }

  @Transactional
  public SubscriptionData resumeRenewal(
    WorkspaceType type,
    String workspaceId
  ) {
    User user = currentUser();
    WorkspaceSubscription subscription = entity(type, workspaceId);
    if (!subscription.getOwnerId().equals(user.getId())) {
      throw new ResourceNotFoundException("Subscription not found");
    }
    if (subscription.getStatus() != WorkspaceSubscription.Status.ACTIVE) {
      throw new InvalidOperationException(
        "Renew the subscription to reactivate it"
      );
    }
    subscription.setCancelAtPeriodEnd(false);
    subscription.setCancelledAt(null);
    return map(subscriptions.save(subscription));
  }

  @Transactional
  public int expireDueSubscriptions() {
    List<WorkspaceSubscription> due =
      subscriptions.findByStatusAndNextBillingAtBefore(
        WorkspaceSubscription.Status.ACTIVE,
        LocalDateTime.now()
      );
    due.forEach(subscription -> {
      subscription.setStatus(
        subscription.isCancelAtPeriodEnd()
          ? WorkspaceSubscription.Status.CANCELLED
          : WorkspaceSubscription.Status.EXPIRED
      );
    });
    subscriptions.saveAll(due);
    return due.size();
  }

  private boolean hasFreeWorkspace(String ownerId, WorkspaceType type) {
    return (
      subscriptions.countFreeSubscriptions(
        ownerId,
        type,
        RESERVED_FREE_STATUSES
      ) >
      0
    );
  }

  private WorkspaceSubscription entity(WorkspaceType type, String workspaceId) {
    return subscriptions
      .findByWorkspaceTypeAndWorkspaceId(type, workspaceId)
      .orElseThrow(() ->
        new ResourceNotFoundException("Subscription not found")
      );
  }

  private SubscriptionData map(WorkspaceSubscription subscription) {
    SubscriptionPlan plan = plans
      .findById(subscription.getPlanId())
      .orElseThrow(() ->
        new ResourceNotFoundException("Subscription plan not found")
      );
    return new SubscriptionData(
      subscription.getId(),
      subscription.getOwnerId(),
      subscription.getWorkspaceType(),
      subscription.getWorkspaceId(),
      subscription.getStatus(),
      subscription.getStartedAt(),
      subscription.getNextBillingAt(),
      subscription.isCancelAtPeriodEnd(),
      subscription.getCancelledAt(),
      planService.map(plan)
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

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
