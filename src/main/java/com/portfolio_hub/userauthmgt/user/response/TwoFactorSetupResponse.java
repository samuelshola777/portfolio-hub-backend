package com.portfolio_hub.userauthmgt.user.response;

import java.time.LocalDateTime;

public record TwoFactorSetupResponse(
  String secret,
  String qrCodeDataUrl,
  LocalDateTime expiresAt
) {}
