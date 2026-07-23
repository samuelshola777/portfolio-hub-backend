package com.portfolio_hub.setuprequest.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetupAssistanceRequest(
  @NotBlank(message = "Enter your full name") @Size(min = 2) String fullName,
  @NotBlank(message = "Enter your email address")
  @Email(message = "Enter a valid email address")
  String email,
  @NotBlank(message = "Enter the WhatsApp number you actively use")
  String whatsAppNumber,
  String message
) {}
