package com.portfolio_hub.enquiry;

import com.portfolio_hub.enquiry.request.EnquiryRequest;
import com.portfolio_hub.enquiry.request.EnquiryStatusRequest;
import com.portfolio_hub.enquiry.response.EnquiryResponse;
import com.portfolio_hub.utils.ApiResponse;
import com.portfolio_hub.utils.PaginatedData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/enquiries")
@RequiredArgsConstructor
public class EnquiryController {

  private final EnquiryService service;

  @PostMapping("/public/{username}")
  public ResponseEntity<ApiResponse<EnquiryResponse>> create(
    @PathVariable String username,
    @Valid @RequestBody EnquiryRequest request,
    HttpServletRequest http
  ) {
    return ResponseEntity.status(201).body(
      ApiResponse.success(
        "Your message has been sent",
        service.create(username, request, http)
      )
    );
  }

  @GetMapping("/private/mine")
  public ResponseEntity<ApiResponse<List<EnquiryResponse>>> mine() {
    return ResponseEntity.ok(
      ApiResponse.success("Enquiries fetched", service.mine())
    );
  }

  @GetMapping("/private/mine/page")
  public ResponseEntity<ApiResponse<PaginatedData<EnquiryResponse>>> minePage(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success("Enquiries fetched", service.pageMine(page, size))
    );
  }

  @PatchMapping("/private/{id}/status")
  public ResponseEntity<ApiResponse<EnquiryResponse>> status(
    @PathVariable String id,
    @Valid @RequestBody EnquiryStatusRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Enquiry updated",
        service.status(id, request.status())
      )
    );
  }
}
