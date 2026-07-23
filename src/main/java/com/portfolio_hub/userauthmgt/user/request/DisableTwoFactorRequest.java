package com.portfolio_hub.userauthmgt.user.request;

import jakarta.validation.constraints.NotBlank;

public record DisableTwoFactorRequest(@NotBlank String password) {}
