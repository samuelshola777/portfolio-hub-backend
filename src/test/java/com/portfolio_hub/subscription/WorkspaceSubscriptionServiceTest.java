package com.portfolio_hub.subscription;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.utils.exception.InvalidOperationException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceSubscriptionServiceTest {

  @Mock
  private WorkspaceSubscriptionRepository subscriptions;

  @Mock
  private SubscriptionPlanRepository plans;

  @Mock
  private SubscriptionPlanService planService;

  @Mock
  private UserRepository users;

  @InjectMocks
  private WorkspaceSubscriptionService service;

  @Test
  void preventsASecondFreeBusinessForTheSameOwner() {
    SubscriptionPlan free = SubscriptionPlan.builder()
      .name("Free Business")
      .code("FREE_BUSINESS")
      .workspaceType(WorkspaceType.BUSINESS)
      .monthlyPrice(BigDecimal.ZERO)
      .currency("NGN")
      .free(true)
      .active(true)
      .build();
    free.setId("free-plan");
    when(
      subscriptions.findByWorkspaceTypeAndWorkspaceId(
        WorkspaceType.BUSINESS,
        "business-2"
      )
    ).thenReturn(Optional.empty());
    when(planService.defaultFreePlan(WorkspaceType.BUSINESS)).thenReturn(free);
    when(
      subscriptions.countFreeSubscriptions(
        "owner-1",
        WorkspaceType.BUSINESS,
        anyList()
      )
    ).thenReturn(1L);

    InvalidOperationException error = assertThrows(
      InvalidOperationException.class,
      () ->
        service.provision("owner-1", WorkspaceType.BUSINESS, "business-2", null)
    );

    assertTrue(error.getMessage().contains("paid plan"));
  }
}
