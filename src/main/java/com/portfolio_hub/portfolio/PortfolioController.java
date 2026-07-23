package com.portfolio_hub.portfolio;

import com.portfolio_hub.analytics.AnalyticsService;
import com.portfolio_hub.analytics.PortfolioEvent;
import com.portfolio_hub.analytics.request.AnalyticsEventRequest;
import com.portfolio_hub.portfolio.request.PortfolioUpdateRequest;
import com.portfolio_hub.portfolio.response.PortfolioResponse;
import com.portfolio_hub.portfolio.response.PublicPortfolioResponse;
import com.portfolio_hub.utils.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

  private final PortfolioService portfolioService;
  private final PortfolioSharingService sharingService;
  private final AnalyticsService analyticsService;

  @GetMapping("/private/mine")
  public ResponseEntity<ApiResponse<PortfolioResponse>> mine() {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Portfolio fetched successfully",
        portfolioService.mine()
      )
    );
  }

  @PatchMapping("/private/mine")
  public ResponseEntity<ApiResponse<PortfolioResponse>> update(
    @Valid @RequestBody PortfolioUpdateRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Portfolio updated successfully",
        portfolioService.updateMine(request)
      )
    );
  }

  @GetMapping("/public/{username}")
  public ResponseEntity<ApiResponse<PublicPortfolioResponse>> publicPortfolio(
    @PathVariable String username
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Portfolio fetched successfully",
        portfolioService.getPublic(username)
      )
    );
  }

  @GetMapping(
    value = "/public/{username}/qr",
    produces = MediaType.IMAGE_PNG_VALUE
  )
  public ResponseEntity<byte[]> qr(@PathVariable String username) {
    return ResponseEntity.ok()
      .contentType(MediaType.IMAGE_PNG)
      .body(sharingService.qrCode(username));
  }

  @GetMapping(
    value = "/public/{username}/export",
    produces = MediaType.APPLICATION_PDF_VALUE
  )
  public ResponseEntity<byte[]> export(
    @PathVariable String username,
    HttpServletRequest request
  ) {
    analyticsService.record(
      username,
      new AnalyticsEventRequest(
        PortfolioEvent.EventType.EXPORT_DOWNLOAD,
        null,
        request.getHeader("Referer")
      ),
      request
    );
    return ResponseEntity.ok()
      .header(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"" + username + "-portfolio.pdf\""
      )
      .contentType(MediaType.APPLICATION_PDF)
      .body(sharingService.portfolioPdf(username));
  }
}
