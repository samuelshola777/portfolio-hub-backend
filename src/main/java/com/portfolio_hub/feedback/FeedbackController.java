package com.portfolio_hub.feedback;

import com.portfolio_hub.feedback.request.FeedbackCreateRequest;
import com.portfolio_hub.feedback.request.FeedbackResponseRequest;
import com.portfolio_hub.feedback.response.FeedbackResponse;
import com.portfolio_hub.utils.ApiResponse;
import com.portfolio_hub.utils.PaginatedData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

  private final FeedbackService feedbackService;

  @PostMapping("/private")
  public ResponseEntity<ApiResponse<FeedbackResponse>> create(
    @Valid @RequestBody FeedbackCreateRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Feedback sent to the Portfolio Hub team",
        feedbackService.create(request)
      )
    );
  }

  @GetMapping("/private/mine")
  public ResponseEntity<ApiResponse<PaginatedData<FeedbackResponse>>> mine(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success("Feedback fetched", feedbackService.mine(page, size))
    );
  }

  @GetMapping("/admin/private")
  public ResponseEntity<ApiResponse<PaginatedData<FeedbackResponse>>> all(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) Feedback.Status status
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Feedback fetched",
        feedbackService.all(page, size, status)
      )
    );
  }

  @PatchMapping("/admin/private/{id}/respond")
  public ResponseEntity<ApiResponse<FeedbackResponse>> respond(
    @PathVariable String id,
    @Valid @RequestBody FeedbackResponseRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Response sent by email and announcement",
        feedbackService.respond(id, request)
      )
    );
  }
}
