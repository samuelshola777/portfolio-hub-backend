package com.portfolio_hub.analytics.response;

import java.time.LocalDate;
import java.util.List;

public record AnalyticsSummaryResponse(
  long views,
  long projectClicks,
  long cvDownloads,
  long socialClicks,
  long websiteClicks,
  long exportDownloads,
  long enquiries,
  List<DailyMetric> dailyViews,
  List<NameCount> trafficSources,
  List<NameCount> locations,
  List<NameCount> topProjects
) {
  public record DailyMetric(LocalDate date, long value) {}

  public record NameCount(String name, long value) {}
}
