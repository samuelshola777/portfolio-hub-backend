package com.portfolio_hub.subscription;

import com.portfolio_hub.subscription.SubscriptionDtos.PlanData;
import com.portfolio_hub.subscription.SubscriptionDtos.SubscriptionData;
import com.portfolio_hub.subscription.SubscriptionDtos.UsageData;
import com.portfolio_hub.utils.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

  private final SubscriptionPlanService plans;
  private final WorkspaceSubscriptionService subscriptions;
  private final WorkspaceUsageService usageService;

  @GetMapping("/public/plans")
  public ResponseEntity<ApiResponse<List<PlanData>>> publicPlans(
    @RequestParam WorkspaceType workspaceType
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Subscription plans fetched",
        plans.publicPlans(workspaceType)
      )
    );
  }

  @GetMapping("/private/mine")
  public ResponseEntity<ApiResponse<List<SubscriptionData>>> mine() {
    return ResponseEntity.ok(
      ApiResponse.success("Subscriptions fetched", subscriptions.mine())
    );
  }

  @GetMapping("/private/workspaces/{workspaceType}/{workspaceId}")
  public ResponseEntity<ApiResponse<SubscriptionData>> workspace(
    @PathVariable WorkspaceType workspaceType,
    @PathVariable String workspaceId
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Subscription fetched",
        subscriptions.mine(workspaceType, workspaceId)
      )
    );
  }

  @GetMapping("/private/workspaces/{workspaceType}/{workspaceId}/usage")
  public ResponseEntity<ApiResponse<UsageData>> usage(
    @PathVariable WorkspaceType workspaceType,
    @PathVariable String workspaceId
  ) {
    subscriptions.mine(workspaceType, workspaceId);
    return ResponseEntity.ok(
      ApiResponse.success(
        "Plan usage fetched",
        usageService.usage(workspaceType, workspaceId)
      )
    );
  }

  @PutMapping("/private/workspaces/{workspaceType}/{workspaceId}/cancel")
  public ResponseEntity<ApiResponse<SubscriptionData>> cancel(
    @PathVariable WorkspaceType workspaceType,
    @PathVariable String workspaceId
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Subscription will end after the current paid period",
        subscriptions.scheduleCancellation(workspaceType, workspaceId)
      )
    );
  }

  @PutMapping("/private/workspaces/{workspaceType}/{workspaceId}/resume")
  public ResponseEntity<ApiResponse<SubscriptionData>> resume(
    @PathVariable WorkspaceType workspaceType,
    @PathVariable String workspaceId
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Subscription renewal restored",
        subscriptions.resumeRenewal(workspaceType, workspaceId)
      )
    );
  }
}
