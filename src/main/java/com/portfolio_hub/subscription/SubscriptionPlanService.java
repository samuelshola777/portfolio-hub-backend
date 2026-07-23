package com.portfolio_hub.subscription;

import com.portfolio_hub.subscription.SubscriptionDtos.*;
import com.portfolio_hub.utils.exception.InvalidInputException;
import com.portfolio_hub.utils.exception.InvalidOperationException;
import com.portfolio_hub.utils.exception.ResourceExistsException;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

  private final SubscriptionPlanRepository plans;
  private final PlanEntitlementRepository entitlements;
  private final WorkspaceSubscriptionRepository subscriptions;

  @Transactional(readOnly = true)
  public List<PlanData> publicPlans(WorkspaceType workspaceType) {
    return mapMany(
      plans.findByWorkspaceTypeAndActiveTrueAndPublicVisibleTrueOrderBySortOrderAsc(
        workspaceType
      )
    );
  }

  @Transactional(readOnly = true)
  public List<PlanData> allPlans() {
    return mapMany(plans.findAllByOrderByWorkspaceTypeAscSortOrderAsc());
  }

  @Transactional
  public PlanData create(PlanRequest request) {
    String code = normalizeCode(request.code());
    if (plans.findByCodeIgnoreCase(code).isPresent()) {
      throw new ResourceExistsException(
        "A subscription plan already uses that code"
      );
    }
    boolean free = Boolean.TRUE.equals(request.free());
    BigDecimal price = money(request.monthlyPrice());
    validateFreePrice(free, price);
    SubscriptionPlan plan = plans.save(
      SubscriptionPlan.builder()
        .code(code)
        .name(required(request.name(), "Plan name"))
        .description(clean(request.description()))
        .workspaceType(request.workspaceType())
        .monthlyPrice(price)
        .currency(currency(request.currency()))
        .free(free)
        .active(request.active() == null || request.active())
        .publicVisible(
          request.publicVisible() == null || request.publicVisible()
        )
        .sortOrder(
          Math.max(0, Objects.requireNonNullElse(request.sortOrder(), 0))
        )
        .build()
    );
    replaceEntitlements(plan.getId(), request.entitlements());
    return get(plan.getId());
  }

  @Transactional
  public PlanData update(String id, PlanRequest request) {
    SubscriptionPlan plan = entity(id);
    if (
      plan.getCode().equals("FREE_PORTFOLIO") ||
      plan.getCode().equals("FREE_BUSINESS")
    ) {
      if (Boolean.FALSE.equals(request.free())) {
        throw new InvalidOperationException(
          "The permanent free plan must remain free"
        );
      }
      if (Boolean.FALSE.equals(request.active())) {
        throw new InvalidOperationException(
          "The permanent free plan must remain active"
        );
      }
    }
    boolean free = request.free() == null ? plan.isFree() : request.free();
    BigDecimal price = request.monthlyPrice() == null
      ? plan.getMonthlyPrice()
      : money(request.monthlyPrice());
    validateFreePrice(free, price);

    if (request.name() != null) plan.setName(
      required(request.name(), "Plan name")
    );
    if (request.description() != null) plan.setDescription(
      clean(request.description())
    );
    if (
      request.workspaceType() != null && subscriptions.countByPlanId(id) > 0
    ) {
      if (request.workspaceType() != plan.getWorkspaceType()) {
        throw new InvalidOperationException(
          "A plan already in use cannot be moved to another workspace type"
        );
      }
    } else if (request.workspaceType() != null) {
      plan.setWorkspaceType(request.workspaceType());
    }
    plan.setMonthlyPrice(price);
    plan.setFree(free);
    if (request.currency() != null) plan.setCurrency(
      currency(request.currency())
    );
    if (request.active() != null) plan.setActive(request.active());
    if (request.publicVisible() != null) {
      plan.setPublicVisible(request.publicVisible());
    }
    if (request.sortOrder() != null) plan.setSortOrder(
      Math.max(0, request.sortOrder())
    );
    plans.save(plan);
    if (request.entitlements() != null) {
      replaceEntitlements(plan.getId(), request.entitlements());
    }
    return get(id);
  }

  @Transactional
  public boolean delete(String id) {
    SubscriptionPlan plan = entity(id);
    if (
      plan.getCode().equals("FREE_PORTFOLIO") ||
      plan.getCode().equals("FREE_BUSINESS")
    ) {
      throw new InvalidOperationException(
        "The permanent free plan cannot be deleted"
      );
    }
    if (subscriptions.countByPlanId(id) > 0) {
      plan.setActive(false);
      plan.setPublicVisible(false);
      plans.save(plan);
      return false;
    }
    entitlements.deleteAllByPlanId(id);
    plans.delete(plan);
    return true;
  }

  @Transactional(readOnly = true)
  public PlanData get(String id) {
    SubscriptionPlan plan = entity(id);
    return map(plan, entitlements.findByPlanIdOrderByCodeAsc(id));
  }

  @Transactional(readOnly = true)
  public SubscriptionPlan defaultFreePlan(WorkspaceType workspaceType) {
    return plans
      .findFirstByWorkspaceTypeAndFreeTrueAndActiveTrueOrderBySortOrderAsc(
        workspaceType
      )
      .orElseThrow(() ->
        new InvalidOperationException(
          "The free " +
            workspaceType.name().toLowerCase() +
            " plan is not available"
        )
      );
  }

  @Transactional(readOnly = true)
  public SubscriptionPlan activePlan(String id, WorkspaceType workspaceType) {
    SubscriptionPlan plan = entity(id);
    if (!plan.isActive() || plan.getWorkspaceType() != workspaceType) {
      throw new InvalidInputException(
        "Choose an active " + workspaceType.name().toLowerCase() + " plan"
      );
    }
    return plan;
  }

  public PlanData map(SubscriptionPlan plan) {
    return map(plan, entitlements.findByPlanIdOrderByCodeAsc(plan.getId()));
  }

  private List<PlanData> mapMany(List<SubscriptionPlan> values) {
    if (values.isEmpty()) return List.of();
    Map<String, List<PlanEntitlement>> byPlan = entitlements
      .findByPlanIdIn(values.stream().map(SubscriptionPlan::getId).toList())
      .stream()
      .collect(Collectors.groupingBy(PlanEntitlement::getPlanId));
    return values
      .stream()
      .map(plan -> map(plan, byPlan.getOrDefault(plan.getId(), List.of())))
      .toList();
  }

  private PlanData map(
    SubscriptionPlan plan,
    List<PlanEntitlement> planEntitlements
  ) {
    return new PlanData(
      plan.getId(),
      plan.getCode(),
      plan.getName(),
      plan.getDescription(),
      plan.getWorkspaceType(),
      plan.getMonthlyPrice(),
      plan.getCurrency(),
      plan.isFree(),
      plan.isActive(),
      plan.isPublicVisible(),
      plan.getSortOrder(),
      planEntitlements
        .stream()
        .sorted(Comparator.comparing(value -> value.getCode().name()))
        .map(value ->
          new EntitlementData(
            value.getCode(),
            value.getCode().displayName(),
            value.getValueType(),
            value.getValue()
          )
        )
        .toList()
    );
  }

  private void replaceEntitlements(
    String planId,
    List<EntitlementRequest> requests
  ) {
    if (requests == null || requests.isEmpty()) {
      throw new InvalidInputException(
        "Add at least one feature or usage limit"
      );
    }
    Set<EntitlementCode> unique = new HashSet<>();
    List<PlanEntitlement> values = requests
      .stream()
      .map(request -> {
        if (!unique.add(request.code())) {
          throw new InvalidInputException(
            request.code().displayName() + " was added more than once"
          );
        }
        String value = validateValue(request.code(), request.value());
        return PlanEntitlement.builder()
          .planId(planId)
          .code(request.code())
          .valueType(request.code().valueType())
          .value(value)
          .build();
      })
      .toList();
    entitlements.deleteAllByPlanId(planId);
    entitlements.flush();
    entitlements.saveAll(values);
  }

  private String validateValue(EntitlementCode code, String raw) {
    String value = required(raw, code.displayName());
    try {
      return switch (code.valueType()) {
        case BOOLEAN -> {
          if (
            !value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")
          ) {
            throw new IllegalArgumentException();
          }
          yield Boolean.toString(Boolean.parseBoolean(value));
        }
        case INTEGER -> {
          int parsed = Integer.parseInt(value);
          if (parsed < -1) throw new IllegalArgumentException();
          yield Integer.toString(parsed);
        }
        case DECIMAL -> new BigDecimal(value)
          .stripTrailingZeros()
          .toPlainString();
        case TEXT -> value;
      };
    } catch (RuntimeException exception) {
      String expectation = code.valueType() == EntitlementValueType.BOOLEAN
        ? "true or false"
        : code.valueType() == EntitlementValueType.INTEGER
          ? "zero, a positive number, or -1 for unlimited"
          : "a valid value";
      throw new InvalidInputException(
        code.displayName() + " must be " + expectation
      );
    }
  }

  private SubscriptionPlan entity(String id) {
    return plans
      .findById(id)
      .orElseThrow(() ->
        new ResourceNotFoundException("Subscription plan not found")
      );
  }

  private String normalizeCode(String value) {
    String code = required(value, "Plan code")
      .toUpperCase(Locale.ROOT)
      .replaceAll("[^A-Z0-9]+", "_")
      .replaceAll("^_|_$", "");
    if (code.length() < 3) {
      throw new InvalidInputException(
        "Plan code must contain at least 3 letters or numbers"
      );
    }
    return code;
  }

  private BigDecimal money(BigDecimal value) {
    return value == null
      ? BigDecimal.ZERO.setScale(2)
      : value.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
  }

  private void validateFreePrice(boolean free, BigDecimal price) {
    if (free && price.compareTo(BigDecimal.ZERO) != 0) {
      throw new InvalidInputException(
        "A free plan must have a monthly price of zero"
      );
    }
    if (!free && price.compareTo(BigDecimal.ZERO) == 0) {
      throw new InvalidInputException(
        "A paid plan must have a monthly price above zero"
      );
    }
  }

  private String currency(String value) {
    String currency = clean(value);
    if (currency == null) return "NGN";
    currency = currency.toUpperCase(Locale.ROOT);
    if (!currency.matches("[A-Z]{3}")) {
      throw new InvalidInputException(
        "Currency must use a three-letter code such as NGN"
      );
    }
    return currency;
  }

  private String required(String value, String field) {
    String clean = clean(value);
    if (clean == null) throw new InvalidInputException(field + " is required");
    return clean;
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
