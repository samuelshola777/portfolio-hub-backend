package com.portfolio_hub.analytics;

import com.portfolio_hub.analytics.request.AnalyticsEventRequest;
import com.portfolio_hub.analytics.response.AnalyticsSummaryResponse;
import com.portfolio_hub.utils.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

  private final AnalyticsService service;

  @PostMapping("/public/{username}/events")
  public ResponseEntity<ApiResponse<Void>> event(
    @PathVariable String username,
    @Valid @RequestBody AnalyticsEventRequest request,
    HttpServletRequest http
  ) {
    service.record(username, request, http);
    return ResponseEntity.ok(ApiResponse.success("Event recorded"));
  }

  @GetMapping("/private/mine")
  public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> mine(
    @RequestParam(defaultValue = "30") int days
  ) {
    return ResponseEntity.ok(
      ApiResponse.success("Analytics fetched", service.mine(days))
    );
  }
}
