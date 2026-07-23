package com.portfolio_hub.profile.request;

import com.portfolio_hub.profile.ProfileEntry;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ProfileEntryRequest(
  @NotNull ProfileEntry.EntryType type,
  @NotBlank String title,
  String organization,
  String subtitle,
  String location,
  LocalDate startDate,
  LocalDate endDate,
  Boolean current,
  String description,
  String url,
  String thumbnailUrl,
  String supportingDocumentUrl,
  Boolean published,
  Integer sortOrder
) {}
