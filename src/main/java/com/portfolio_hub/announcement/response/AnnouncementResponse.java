package com.portfolio_hub.announcement.response;

import com.portfolio_hub.announcement.request.AnnouncementRequest;
import java.time.LocalDateTime;
import java.util.List;

public record AnnouncementResponse(
  String recipientId,
  String announcementId,
  String subject,
  String message,
  List<AnnouncementRequest.Attachment> attachments,
  LocalDateTime createdAt,
  LocalDateTime readAt
) {}
