package com.portfolio_hub.utils.fileupload.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record FileDeleteRequest(
  @NotEmpty(message = "Select at least one file") List<String> fileUrls
) {}
