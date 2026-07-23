package com.portfolio_hub.profile.request;

import com.portfolio_hub.profile.Skill;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SkillRequest(
  @NotBlank String name,
  @NotBlank String category,
  @NotNull Skill.Proficiency proficiency,
  String iconUrl,
  Boolean featured,
  Integer sortOrder
) {}
