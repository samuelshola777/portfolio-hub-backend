package com.portfolio_hub.userauthmgt.user.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank(message = "Google credential is required") String credential
) {}