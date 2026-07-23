package com.portfolio_hub.profile.request;

import jakarta.validation.constraints.NotBlank;

public record SocialLinkRequest(
  @NotBlank String platform,
  @NotBlank String url,
  Integer sortOrder
) {}
