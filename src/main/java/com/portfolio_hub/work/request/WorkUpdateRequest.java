package com.portfolio_hub.work.request;

import com.portfolio_hub.work.Work;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record WorkUpdateRequest(
  @Size(min = 2) String title,
  @Size(min = 10) String summary,
  String description,
  String challenge,
  String process,
  String results,
  @Size(min = 2) String category,
  String role,
  LocalDate startedAt,
  LocalDate completedAt,
  Boolean ongoing,
  String projectUrl,
  String sourceUrl,
  String thumbnailUrl,
  List<String> galleryUrls,
  List<String> technologyStack,
  Boolean featured,
  Integer sortOrder,
  Work.PublicationStatus status
) {}
