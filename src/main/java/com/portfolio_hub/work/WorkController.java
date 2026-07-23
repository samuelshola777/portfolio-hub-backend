package com.portfolio_hub.work;

import com.portfolio_hub.utils.ApiResponse;
import com.portfolio_hub.utils.PaginatedData;
import com.portfolio_hub.work.request.WorkCreateRequest;
import com.portfolio_hub.work.request.WorkUpdateRequest;
import com.portfolio_hub.work.response.WorkResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/works")
@RequiredArgsConstructor
public class WorkController {

  private final WorkService workService;

  @GetMapping("/private")
  public ResponseEntity<ApiResponse<List<WorkResponse>>> list() {
    return ResponseEntity.ok(
      ApiResponse.success("Works fetched successfully", workService.listMine())
    );
  }

  @GetMapping("/private/page")
  public ResponseEntity<ApiResponse<PaginatedData<WorkResponse>>> page(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Works fetched successfully",
        workService.pageMine(page, size)
      )
    );
  }

  @PostMapping("/private")
  public ResponseEntity<ApiResponse<WorkResponse>> create(
    @Valid @RequestBody WorkCreateRequest request
  ) {
    return ResponseEntity.status(201).body(
      ApiResponse.success(
        "Work created successfully",
        workService.create(request)
      )
    );
  }

  @PatchMapping("/private/{workId}")
  public ResponseEntity<ApiResponse<WorkResponse>> update(
    @PathVariable String workId,
    @Valid @RequestBody WorkUpdateRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Work updated successfully",
        workService.update(workId, request)
      )
    );
  }

  @DeleteMapping("/private/{workId}")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String workId) {
    workService.delete(workId);
    return ResponseEntity.ok(ApiResponse.success("Work deleted successfully"));
  }
}
