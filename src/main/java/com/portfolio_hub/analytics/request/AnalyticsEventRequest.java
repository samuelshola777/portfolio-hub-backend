package com.portfolio_hub.analytics.request;

import com.portfolio_hub.analytics.PortfolioEvent;
import jakarta.validation.constraints.NotNull;

public record AnalyticsEventRequest(
  @NotNull PortfolioEvent.EventType eventType,
  String targetId,
  String source
) {}
