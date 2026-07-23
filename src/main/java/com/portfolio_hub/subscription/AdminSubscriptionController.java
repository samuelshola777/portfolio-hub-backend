package com.portfolio_hub.subscription;

import com.portfolio_hub.business.BusinessRepository;
import com.portfolio_hub.portfolio.PortfolioRepository;
import com.portfolio_hub.subscription.SubscriptionDtos.*;
import com.portfolio_hub.utils.ApiResponse;
import com.portfolio_hub.utils.exception.InvalidInputException;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@RequiredArgsConstructor
public class AdminSubscriptionController {

  private final SubscriptionPlanService plans;
  private final WorkspaceSubscriptionService subscriptions;
  private final BusinessRepository businesses;
  private final PortfolioRepository portfolios;

  @GetMapping("/plans")
  public ResponseEntity<ApiResponse<List<PlanData>>> plans() {
    return ResponseEntity.ok(
      ApiResponse.success("Subscription plans fetched", plans.allPlans())
    );
  }

  @PostMapping("/plans")
  public ResponseEntity<ApiResponse<PlanData>> createPlan(
    @Valid @RequestBody PlanRequest request
  ) {
    return ResponseEntity.status(201).body(
      ApiResponse.success("Subscription plan created", plans.create(request))
    );
  }

  @PatchMapping("/plans/{id}")
  public ResponseEntity<ApiResponse<PlanData>> updatePlan(
    @PathVariable String id,
    @RequestBody PlanRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Subscription plan updated",
        plans.update(id, request)
      )
    );
  }

  @DeleteMapping("/plans/{id}")
  public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable String id) {
    boolean deleted = plans.delete(id);
    return ResponseEntity.ok(
      ApiResponse.success(
        deleted
          ? "Subscription plan deleted"
          : "Subscription plan archived because businesses are using it"
      )
    );
  }

  @PutMapping("/assign")
  public ResponseEntity<ApiResponse<SubscriptionData>> assign(
    @Valid @RequestBody AssignmentRequest request
  ) {
    String ownerId = ownerId(request.workspaceType(), request.workspaceId());
    return ResponseEntity.ok(
      ApiResponse.success(
        "Workspace subscription updated",
        subscriptions.assign(
          ownerId,
          request.workspaceType(),
          request.workspaceId(),
          request.planId(),
          request.status(),
          request.adminNote()
        )
      )
    );
  }

  private String ownerId(WorkspaceType type, String workspaceId) {
    if (type == null) throw new InvalidInputException(
      "Workspace type is required"
    );
    return switch (type) {
      case BUSINESS -> businesses
        .findById(workspaceId)
        .orElseThrow(() -> new ResourceNotFoundException("Business not found"))
        .getOwnerId();
      case PORTFOLIO -> portfolios
        .findById(workspaceId)
        .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"))
        .getOwnerId();
    };
  }
}
