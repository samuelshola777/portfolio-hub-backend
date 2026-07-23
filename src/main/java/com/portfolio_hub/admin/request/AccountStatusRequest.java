package com.portfolio_hub.admin.request;

import com.portfolio_hub.userauthmgt.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountStatusRequest(
  @NotNull User.AccountStatus status,
  @NotBlank @Size(min = 3) String reason
) {}
