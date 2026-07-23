package com.portfolio_hub.portfolio.response;

import com.portfolio_hub.portfolio.Portfolio;
import com.portfolio_hub.profile.response.ProfileEntryResponse;
import com.portfolio_hub.profile.response.SkillResponse;
import com.portfolio_hub.profile.response.SocialLinkResponse;
import com.portfolio_hub.work.response.WorkResponse;
import java.util.List;

public record PublicPortfolioResponse(
  String username,
  String fullName,
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
  List<WorkResponse> works,
  List<ProfileEntryResponse> profileEntries,
  List<SkillResponse> skills,
  List<SocialLinkResponse> socialLinks
) {}
