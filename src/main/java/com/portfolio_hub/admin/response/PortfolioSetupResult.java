package com.portfolio_hub.admin.response;

import com.portfolio_hub.userauthmgt.user.response.UserResponse;

public record PortfolioSetupResult(
  UserResponse user,
  boolean accountCreated,
  int skillsSaved,
  int backgroundEntriesSaved,
  int projectsSaved,
  int socialLinksSaved
) {}
