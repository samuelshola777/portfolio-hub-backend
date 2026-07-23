package com.portfolio_hub;

import java.time.Instant;

public record KeepAliveResponse(
  String status,
  String message,
  Instant timestamp
) {}
