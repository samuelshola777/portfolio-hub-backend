package com.portfolio_hub.profile.response;

import com.portfolio_hub.profile.Skill;

public record SkillResponse(
  String id,
  String name,
  String category,
  Skill.Proficiency proficiency,
  String iconUrl,
  boolean featured,
  int sortOrder
) {}
