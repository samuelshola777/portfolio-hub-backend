package com.portfolio_hub.enquiry.response;

import com.portfolio_hub.enquiry.Enquiry;
import java.time.LocalDateTime;

public record EnquiryResponse(
  String id,
  String name,
  String email,
  String company,
  String message,
  Enquiry.Status status,
  LocalDateTime createdAt
) {}
