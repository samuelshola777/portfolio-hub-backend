package com.portfolio_hub.admin.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
  @NotBlank @Size(min = 2) String fullName,
  @NotBlank @Email String email,
  @NotBlank
  @Pattern(regexp = "^\\S+$", message = "Username cannot contain spaces")
  String username,
  @NotBlank(message = "Enter the user's active WhatsApp number")
  String whatsAppNumber,
  @NotNull Boolean emailVerified
) {}
