package com.portfolio_hub.admin.response;

import java.time.LocalDate;
import java.util.List;

public record AdminAnalyticsResponse(
  long totalUsers,
  long activeUsers,
  long verifiedUsers,
  long publishedPortfolios,
  long totalStorageBytes,
  long portfolioViews,
  long projectClicks,
  long totalEnquiries,
  long newEnquiries,
  List<DailyGrowth> userGrowth,
  boolean activityAvailable
) {
  public record DailyGrowth(LocalDate date, long users) {}
}
