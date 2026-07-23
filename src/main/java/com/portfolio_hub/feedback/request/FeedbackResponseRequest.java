package com.portfolio_hub.feedback.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedbackResponseRequest(
  @NotBlank @Size(min = 2) String response
) {}
