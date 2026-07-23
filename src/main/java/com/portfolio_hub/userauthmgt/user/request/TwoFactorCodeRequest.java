package com.portfolio_hub.userauthmgt.user.request;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorCodeRequest(@NotBlank String code) {}
