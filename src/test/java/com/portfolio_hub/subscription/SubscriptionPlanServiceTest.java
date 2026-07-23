package com.portfolio_hub.subscription;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.portfolio_hub.subscription.SubscriptionDtos.EntitlementRequest;
import com.portfolio_hub.subscription.SubscriptionDtos.PlanRequest;
import com.portfolio_hub.utils.exception.InvalidInputException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionPlanServiceTest {

  @Mock
  private SubscriptionPlanRepository plans;

  @Mock
  private PlanEntitlementRepository entitlements;

  @Mock
  private WorkspaceSubscriptionRepository subscriptions;

  @InjectMocks
  private SubscriptionPlanService service;

  @BeforeEach
  void savePlanWithAnId() {
    lenient()
      .when(plans.save(any(SubscriptionPlan.class)))
      .thenAnswer(invocation -> {
        SubscriptionPlan plan = invocation.getArgument(0);
        plan.setId("plan-1");
        return plan;
      });
  }

  @Test
  void createsAFreeBusinessPlanWithTypedEntitlements() {
    when(plans.findByCodeIgnoreCase("FREE_STARTER")).thenReturn(
      Optional.empty()
    );
    when(plans.findById("plan-1")).thenAnswer(invocation -> {
      SubscriptionPlan plan = SubscriptionPlan.builder()
        .code("FREE_STARTER")
        .name("Free Starter")
        .workspaceType(WorkspaceType.BUSINESS)
        .monthlyPrice(BigDecimal.ZERO.setScale(2))
        .currency("NGN")
        .free(true)
        .active(true)
        .publicVisible(true)
        .build();
      plan.setId("plan-1");
      return Optional.of(plan);
    });
    when(entitlements.findByPlanIdOrderByCodeAsc("plan-1")).thenReturn(
      List.of()
    );

    var result = service.create(
      new PlanRequest(
        "free starter",
        "Free Starter",
        null,
        WorkspaceType.BUSINESS,
        BigDecimal.ZERO,
        "ngn",
        true,
        true,
        true,
        0,
        List.of(
          new EntitlementRequest(EntitlementCode.PRODUCTS, "5"),
          new EntitlementRequest(EntitlementCode.CUSTOM_DOMAIN, "false")
        )
      )
    );

    assertEquals("FREE_STARTER", result.code());
    assertTrue(result.free());
    verify(entitlements).saveAll(any());
  }

  @Test
  void rejectsAFreePlanWithAPrice() {
    when(plans.findByCodeIgnoreCase("FREE_WRONG")).thenReturn(Optional.empty());

    InvalidInputException error = assertThrows(
      InvalidInputException.class,
      () ->
        service.create(
          new PlanRequest(
            "free wrong",
            "Free Wrong",
            null,
            WorkspaceType.BUSINESS,
            BigDecimal.TEN,
            "NGN",
            true,
            true,
            true,
            0,
            List.of(new EntitlementRequest(EntitlementCode.PRODUCTS, "5"))
          )
        )
    );

    assertTrue(error.getMessage().contains("monthly price of zero"));
    verify(plans, never()).save(any());
  }

  @Test
  void rejectsAnInvalidBooleanEntitlement() {
    when(plans.findByCodeIgnoreCase("STARTER")).thenReturn(Optional.empty());

    InvalidInputException error = assertThrows(
      InvalidInputException.class,
      () ->
        service.create(
          new PlanRequest(
            "starter",
            "Starter",
            null,
            WorkspaceType.BUSINESS,
            BigDecimal.valueOf(5000),
            "NGN",
            false,
            true,
            true,
            0,
            List.of(
              new EntitlementRequest(EntitlementCode.CUSTOM_DOMAIN, "sometimes")
            )
          )
        )
    );

    assertTrue(error.getMessage().contains("true or false"));
  }
}
