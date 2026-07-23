package com.portfolio_hub.portfolio.request;

import com.portfolio_hub.portfolio.Portfolio;
import jakarta.validation.constraints.Pattern;

public record PortfolioUpdateRequest(
  String headline,
  String introduction,
  String note,
  String availability,
  String avatarUrl,
  String cvUrl,
  String introVideoUrl,
  String websiteUrl,
  @Pattern(
    regexp = "^[A-Za-z0-9-]*$",
    message = "Enter only the GitHub username"
  )
  String githubUsername,
  Portfolio.Theme theme,
  @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String accent,
  @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String background,
  Portfolio.FontStyle font,
  Portfolio.Motion motion,
  Portfolio.PublicationStatus status
) {}
