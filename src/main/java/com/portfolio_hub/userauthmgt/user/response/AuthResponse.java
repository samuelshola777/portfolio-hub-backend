package com.portfolio_hub.userauthmgt.user.response;

public record AuthResponse(
  String accessToken,
  String refreshToken,
  UserResponse user
) {}
