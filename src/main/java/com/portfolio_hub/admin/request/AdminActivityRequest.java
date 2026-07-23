package com.portfolio_hub.admin.request;

import jakarta.validation.constraints.NotBlank;

public record AdminActivityRequest(
  @NotBlank String action,
  @NotBlank String description,
  String targetUserId
) {}
