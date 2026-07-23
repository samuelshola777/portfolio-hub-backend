package com.portfolio_hub.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AdminActivityDeleteRequest(
  @NotEmpty List<@NotBlank String> ids
) {}
