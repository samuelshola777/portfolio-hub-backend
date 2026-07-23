package com.portfolio_hub.userauthmgt.user;

public record VerificationEmailRequested(
  String email,
  String fullName,
  String verificationUrl
) {}
