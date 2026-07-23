package com.portfolio_hub.userauthmgt.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
  @NotBlank String token,
  @NotBlank @Size(min = 5) String password
) {}
