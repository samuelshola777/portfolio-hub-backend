package com.portfolio_hub.announcement.request;

import com.portfolio_hub.userauthmgt.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AnnouncementRequest(
  List<@NotBlank String> userIds,
  boolean allMatching,
  List<@NotBlank String> excludedUserIds,
  String recipientSearch,
  User.AccountStatus recipientStatus,
  Boolean recipientVerified,
  User.UserRole recipientRole,
  @NotBlank String subject,
  @NotBlank String message,
  @Valid List<Attachment> attachments
) {
  public record Attachment(
    @NotBlank String url,
    @NotBlank String name,
    String contentType,
    Long size
  ) {}
}
