package com.portfolio_hub.profile.response;

import com.portfolio_hub.profile.ProfileEntry;
import java.time.LocalDate;

public record ProfileEntryResponse(
  String id,
  ProfileEntry.EntryType type,
  String title,
  String organization,
  String subtitle,
  String location,
  LocalDate startDate,
  LocalDate endDate,
  boolean current,
  String description,
  String url,
  String thumbnailUrl,
  String supportingDocumentUrl,
  boolean published,
  int sortOrder
) {}
