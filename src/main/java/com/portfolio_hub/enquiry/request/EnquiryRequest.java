package com.portfolio_hub.enquiry.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnquiryRequest(
  @NotBlank String name,
  @NotBlank @Email String email,
  String company,
  @NotBlank @Size(min = 10) String message
) {}
