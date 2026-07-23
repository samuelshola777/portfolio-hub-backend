package com.portfolio_hub.billing;

import com.portfolio_hub.subscription.WorkspaceSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionLifecycleScheduler {

  private final BillingService billing;
  private final WorkspaceSubscriptionService subscriptions;

  @Scheduled(cron = "${application.billing.lifecycle-cron:0 15 * * * *}")
  public void maintainBillingLifecycle() {
    int payments = billing.expireAbandonedPayments();
    int subscriptionsExpired = subscriptions.expireDueSubscriptions();
    if (payments + subscriptionsExpired > 0) {
      log.info(
        "Billing lifecycle updated {} payments and {} subscriptions",
        payments,
        subscriptionsExpired
      );
    }
  }
}
