package com.portfolio_hub.work.response;

import com.portfolio_hub.work.Work;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record WorkResponse(
  String id,
  String title,
  String slug,
  String summary,
  String description,
  String challenge,
  String process,
  String results,
  String category,
  String role,
  LocalDate startedAt,
  LocalDate completedAt,
  boolean ongoing,
  String projectUrl,
  String sourceUrl,
  String thumbnailUrl,
  List<String> galleryUrls,
  List<String> technologyStack,
  boolean featured,
  int sortOrder,
  Work.PublicationStatus status,
  LocalDateTime createdAt,
  LocalDateTime updatedAt
) {}
