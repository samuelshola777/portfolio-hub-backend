package com.portfolio_hub.userauthmgt.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CurrentUserUpdateRequest(
  @NotBlank(message = "Full name is required") @Size(min = 2) String fullName,
  @NotBlank(message = "Enter the WhatsApp number you actively use")
  String whatsAppNumber
) {}
