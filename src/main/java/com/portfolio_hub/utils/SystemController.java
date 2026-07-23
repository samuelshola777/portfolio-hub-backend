package com.portfolio_hub.utils;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

  @GetMapping("/public/health")
  public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Portfolio Hub API is healthy",
        Map.of("status", "UP", "time", Instant.now())
      )
    );
  }
}
