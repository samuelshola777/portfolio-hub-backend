package com.portfolio_hub.userauthmgt.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
  @NotBlank String currentPassword,
  @NotBlank @Size(min = 5) String newPassword
) {}
