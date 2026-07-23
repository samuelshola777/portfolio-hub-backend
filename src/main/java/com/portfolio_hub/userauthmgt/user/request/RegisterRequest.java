package com.portfolio_hub.userauthmgt.user.request;

import com.portfolio_hub.userauthmgt.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
  @NotBlank(message = "Full name is required") @Size(min = 2) String fullName,
  @NotBlank(message = "Email address is required") @Email String email,
  @NotBlank(message = "Username is required")
  @Size(min = 5)
  @Pattern(
    regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{3,}[A-Za-z0-9]$",
    message = "Username must use at least 5 letters, numbers, hyphens or underscores"
  )
  String username,
  @NotBlank(message = "Enter the WhatsApp number you actively use")
  String whatsAppNumber,
  @NotBlank(message = "Password is required") @Size(min = 5) String password,
  User.UserRole accountType
) {}
