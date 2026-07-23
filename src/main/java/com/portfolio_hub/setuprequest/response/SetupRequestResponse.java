package com.portfolio_hub.setuprequest.response;

import com.portfolio_hub.setuprequest.PortfolioSetupRequest;
import java.time.LocalDateTime;

public record SetupRequestResponse(
  String id,
  String fullName,
  String email,
  String whatsAppNumber,
  String message,
  PortfolioSetupRequest.Status status,
  String adminNote,
  PortfolioSetupRequest.TargetType targetType,
  String ownerId,
  String workspaceId,
  LocalDateTime createdAt,
  LocalDateTime updatedAt
) {}
