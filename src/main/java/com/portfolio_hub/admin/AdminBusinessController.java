package com.portfolio_hub.admin;

import com.portfolio_hub.business.*;
import com.portfolio_hub.utils.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/businesses")
@RequiredArgsConstructor
public class AdminBusinessController {

  private final BusinessRepository businesses;
  private final BusinessService businessService;

  @PostMapping
  public ResponseEntity<ApiResponse<BusinessService.BusinessData>> create(
    @Valid @RequestBody CreateBusinessRequest request
  ) {
    return ResponseEntity.status(201).body(
      ApiResponse.success(
        "Business created for the user",
        businessService.createForOwner(request.ownerId(), request.business())
      )
    );
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PaginatedData<Business>>> list(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    var result = businesses.findAll(
      PageRequest.of(
        Math.max(0, page - 1),
        Math.min(100, Math.max(1, size)),
        Sort.by(Sort.Direction.DESC, "createdAt")
      )
    );
    return ResponseEntity.ok(
      ApiResponse.success(
        "Businesses fetched",
        PaginatedData.from(result, value -> value)
      )
    );
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<ApiResponse<Business>> status(
    @PathVariable String id,
    @RequestBody StatusRequest request
  ) {
    Business business = businesses
      .findById(id)
      .orElseThrow(() ->
        new com.portfolio_hub.utils.exception.ResourceNotFoundException(
          "Business not found"
        )
      );
    business.setStatus(request.status());
    return ResponseEntity.ok(
      ApiResponse.success("Business status updated", businesses.save(business))
    );
  }

  public record StatusRequest(Business.Status status) {}

  public record CreateBusinessRequest(
    @NotBlank String ownerId,
    @NotNull @Valid BusinessController.BusinessRequest business
  ) {}
}
