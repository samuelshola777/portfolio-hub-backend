package com.portfolio_hub.feedback.response;

import com.portfolio_hub.feedback.Feedback;
import java.time.LocalDateTime;

public record FeedbackResponse(
  String id,
  String ownerId,
  String userName,
  String userEmail,
  Feedback.Category category,
  String subject,
  String message,
  Feedback.Status status,
  String adminResponse,
  LocalDateTime createdAt,
  LocalDateTime respondedAt
) {}
