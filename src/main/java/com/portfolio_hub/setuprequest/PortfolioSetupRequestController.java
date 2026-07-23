package com.portfolio_hub.setuprequest;

import com.portfolio_hub.setuprequest.request.SetupAssistanceRequest;
import com.portfolio_hub.setuprequest.request.SetupRequestStatusUpdate;
import com.portfolio_hub.setuprequest.response.SetupRequestResponse;
import com.portfolio_hub.utils.ApiResponse;
import com.portfolio_hub.utils.PaginatedData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/setup-requests")
@RequiredArgsConstructor
public class PortfolioSetupRequestController {

  private final PortfolioSetupRequestService service;

  @PostMapping("/public")
  public ResponseEntity<ApiResponse<SetupRequestResponse>> requestHelp(
    @Valid @RequestBody SetupAssistanceRequest request
  ) {
    return ResponseEntity.status(201).body(
      ApiResponse.success(
        "Your request has been received. An administrator will contact you using your email or WhatsApp number",
        service.create(request)
      )
    );
  }

  @PostMapping("/private/business/{businessId}")
  public ResponseEntity<ApiResponse<SetupRequestResponse>> requestBusinessHelp(
    @PathVariable String businessId,
    @RequestBody(required = false) BusinessHelpRequest request
  ) {
    String message = request == null ? null : request.message();
    return ResponseEntity.status(201).body(
      ApiResponse.success(
        "Your request has been received. An administrator will contact you by email or WhatsApp",
        service.createForBusiness(businessId, message)
      )
    );
  }

  @GetMapping("/admin/private")
  public ResponseEntity<
    ApiResponse<PaginatedData<SetupRequestResponse>>
  > requests(
    @RequestParam(required = false) PortfolioSetupRequest.Status status,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Setup requests fetched",
        service.list(status, page, size)
      )
    );
  }

  @PatchMapping("/admin/private/{id}")
  public ResponseEntity<ApiResponse<SetupRequestResponse>> update(
    @PathVariable String id,
    @Valid @RequestBody SetupRequestStatusUpdate request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success("Setup request updated", service.update(id, request))
    );
  }

  public record BusinessHelpRequest(String message) {}
}
