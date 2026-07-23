package com.portfolio_hub.enquiry.request;

import com.portfolio_hub.enquiry.Enquiry;
import jakarta.validation.constraints.NotNull;

public record EnquiryStatusRequest(@NotNull Enquiry.Status status) {}
