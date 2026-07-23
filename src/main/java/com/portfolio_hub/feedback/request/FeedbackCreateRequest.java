package com.portfolio_hub.feedback.request;

import com.portfolio_hub.feedback.Feedback;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackCreateRequest(
  @NotNull Feedback.Category category,
  @NotBlank String subject,
  @NotBlank @Size(min = 5) String message
) {}
