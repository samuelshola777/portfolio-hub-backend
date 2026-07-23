package com.portfolio_hub.admin.response;

import com.portfolio_hub.portfolio.response.PortfolioResponse;
import com.portfolio_hub.userauthmgt.user.response.UserResponse;
import java.time.LocalDateTime;

public record AdminUserDetailResponse(
  UserResponse account,
  LocalDateTime updatedAt,
  String portfolioLink,
  PortfolioResponse portfolio,
  ContentCounts counts
) {
  public record ContentCounts(
    long projects,
    long backgroundEntries,
    long skills,
    long socialLinks,
    long enquiries,
    long analyticsEvents,
    long files,
    long businesses,
    long businessContent,
    long businessOrders,
    long businessEnquiries
  ) {}
}
