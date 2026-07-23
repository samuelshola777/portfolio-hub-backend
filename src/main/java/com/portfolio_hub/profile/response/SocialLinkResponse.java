package com.portfolio_hub.profile.response;

public record SocialLinkResponse(
  String id,
  String platform,
  String url,
  int sortOrder
) {}
