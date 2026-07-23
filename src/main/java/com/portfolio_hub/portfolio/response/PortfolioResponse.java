package com.portfolio_hub.portfolio.response;

import com.portfolio_hub.portfolio.Portfolio;
import java.time.LocalDateTime;

public record PortfolioResponse(
  String id,
  String username,
  String headline,
  String introduction,
  String note,
  String availability,
  String avatarUrl,
  String cvUrl,
  String introVideoUrl,
  String websiteUrl,
  String githubUsername,
  Portfolio.Theme theme,
  String accent,
  String background,
  Portfolio.FontStyle font,
  Portfolio.Motion motion,
  Portfolio.PublicationStatus status,
  LocalDateTime publishedAt
) {}
