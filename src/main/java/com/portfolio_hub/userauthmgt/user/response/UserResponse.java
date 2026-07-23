package com.portfolio_hub.userauthmgt.user.response;

import com.portfolio_hub.userauthmgt.user.User;
import java.time.LocalDateTime;

public record UserResponse(
  String id,
  String fullName,
  String email,
  String username,
  String whatsAppNumber,
  User.UserRole role,
  User.AccountStatus status,
  boolean emailVerified,
  boolean twoFactorEnabled,
  LocalDateTime createdAt,
  LocalDateTime lastLoginAt
) {}
