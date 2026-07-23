package com.portfolio_hub.setuprequest.request;

import com.portfolio_hub.setuprequest.PortfolioSetupRequest;
import jakarta.validation.constraints.NotNull;

public record SetupRequestStatusUpdate(
  @NotNull(message = "Choose the request status")
  PortfolioSetupRequest.Status status,
  String adminNote
) {}
