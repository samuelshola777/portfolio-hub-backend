package com.portfolio_hub.github;

public record GitHubRepositoryResponse(
  String name,
  String description,
  String url,
  String language,
  int stars,
  int forks,
  String updatedAt
) {}
