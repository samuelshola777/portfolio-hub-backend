package com.portfolio_hub.profile.response;

import java.util.List;

public record ProfileContentResponse(
  List<ProfileEntryResponse> entries,
  List<SkillResponse> skills,
  List<SocialLinkResponse> socialLinks
) {}
