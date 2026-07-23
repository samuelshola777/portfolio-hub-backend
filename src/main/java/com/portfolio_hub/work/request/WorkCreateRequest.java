package com.portfolio_hub.work.request;

import com.portfolio_hub.work.Work;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record WorkCreateRequest(
  @NotBlank @Size(min = 2) String title,
  @NotBlank
  @Size(min = 2)
  @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$")
  String slug,
  @NotBlank @Size(min = 10) String summary,
  String description,
  String challenge,
  String process,
  String results,
  @NotBlank @Size(min = 2) String category,
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
  Work.PublicationStatus status
) {}
