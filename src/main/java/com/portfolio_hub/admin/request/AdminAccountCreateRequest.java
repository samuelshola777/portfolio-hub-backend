package com.portfolio_hub.admin.request;

import com.portfolio_hub.userauthmgt.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminAccountCreateRequest(
  @NotBlank(message = "Enter the user's full name")
  @Size(min = 2)
  String fullName,
  @NotBlank(message = "Enter the user's email address")
  @Email(message = "Enter a valid email address")
  String email,
  @NotBlank(message = "Enter the user's active WhatsApp number")
  String whatsAppNumber,
  @NotBlank(message = "Choose a username")
  @Size(min = 5, message = "The username must contain at least 5 characters")
  @Pattern(
    regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{3,}[A-Za-z0-9]$",
    message = "Use letters, numbers, hyphens or underscores for the username"
  )
  String username,
  User.UserRole accountType
) {}
