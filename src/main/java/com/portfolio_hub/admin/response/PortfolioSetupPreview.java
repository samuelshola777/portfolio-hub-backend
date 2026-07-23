package com.portfolio_hub.admin.response;

import java.util.List;

public record PortfolioSetupPreview(
  boolean valid,
  boolean existingAccount,
  String existingUserId,
  String fullName,
  String email,
  String whatsAppNumber,
  String username,
  int skills,
  int backgroundEntries,
  int projects,
  int socialLinks,
  List<String> warnings,
  List<String> errors
) {}
