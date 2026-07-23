package com.portfolio_hub.admin.response;

import java.time.LocalDateTime;

public record AdminActivityResponse(
  String id,
  String actorId,
  String targetUserId,
  String action,
  String description,
  LocalDateTime createdAt
) {}
