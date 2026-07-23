package com.portfolio_hub.announcement.response;

public record AnnouncementSendResponse(
  String announcementId,
  int recipientCount
) {}
