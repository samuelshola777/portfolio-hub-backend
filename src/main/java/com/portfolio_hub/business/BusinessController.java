package com.portfolio_hub.business;

import com.portfolio_hub.utils.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/businesses")
@RequiredArgsConstructor
public class BusinessController {

  private final BusinessService service;

  public record BusinessRequest(
    String slug,
    String name,
    String tagline,
    String description,
    String industry,
    String category,
    @Min(1800) @Max(2200) Integer yearEstablished,
    String companySize,
    String registrationNumber,
    String logoUrl,
    String coverUrl,
    @Email String email,
    String phone,
    String websiteUrl,
    String address,
    String socialLinksJson,
    String introVideoUrl,
    String templateKey,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String accentColor,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String lightBackground,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String darkBackground,
    Business.ThemeMode defaultMode,
    Business.Status status,
    String subscriptionPlanId,
    Business.OnboardingStage onboardingStage
  ) {}

  public record ItemRequest(
    BusinessItem.Type type,
    String title,
    String category,
    String summary,
    String description,
    String thumbnailUrl,
    String mediaJson,
    String configurationJson,
    @DecimalMin("0.0") BigDecimal price,
    @DecimalMin("0.0") BigDecimal discountPrice,
    @Min(0) Integer quantity,
    Boolean featured,
    @Min(0) Integer sortOrder,
    BusinessItem.Status status
  ) {}

  public record OrderRequest(
    String itemId,
    @NotBlank String itemName,
    @NotBlank String customerName,
    @NotBlank @Email String customerEmail,
    String customerPhone,
    String address,
    @Positive int quantity,
    String variation,
    String instructions
  ) {}

  public record EnquiryRequest(
    BusinessEnquiry.Type type,
    @NotBlank String name,
    @NotBlank @Email String email,
    String company,
    String phone,
    @NotBlank @Size(min = 2) String message
  ) {}

  public record StatusRequest(@NotBlank String status) {}

  @GetMapping("/private")
  public ResponseEntity<
    ApiResponse<List<BusinessService.BusinessData>>
  > mine() {
    return ResponseEntity.ok(
      ApiResponse.success("Businesses fetched", service.mine())
    );
  }

  @PostMapping("/private")
  public ResponseEntity<ApiResponse<BusinessService.BusinessData>> create(
    @Valid @RequestBody BusinessRequest r
  ) {
    return ResponseEntity.status(201).body(
      ApiResponse.success("Business created", service.create(r))
    );
  }

  @PatchMapping("/private/{id}")
  public ResponseEntity<ApiResponse<BusinessService.BusinessData>> update(
    @PathVariable String id,
    @Valid @RequestBody BusinessRequest r
  ) {
    return ResponseEntity.ok(
      ApiResponse.success("Business updated", service.update(id, r))
    );
  }

  @GetMapping("/private/{id}/onboarding")
  public ResponseEntity<ApiResponse<BusinessService.OnboardingData>> onboarding(
    @PathVariable String id
  ) {
    return ResponseEntity.ok(
      ApiResponse.success("Setup progress fetched", service.onboarding(id))
    );
  }

  @PostMapping("/private/{id}/publish")
  public ResponseEntity<ApiResponse<BusinessService.BusinessData>> publish(
    @PathVariable String id
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Your business website is now live",
        service.publish(id)
      )
    );
  }

  @GetMapping("/public/{slug}")
  public ResponseEntity<ApiResponse<BusinessService.BusinessData>> one(
    @PathVariable String slug
  ) {
    return ResponseEntity.ok(
      ApiResponse.success("Business fetched", service.publicBusiness(slug))
    );
  }

  @GetMapping("/private/{id}/items")
  public ResponseEntity<
    ApiResponse<PaginatedData<BusinessService.ItemData>>
  > items(
    @PathVariable String id,
    @RequestParam BusinessItem.Type type,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Content fetched",
        service.listItems(id, type, page, size, false)
      )
    );
  }

  @PostMapping("/private/{id}/items")
  public ResponseEntity<ApiResponse<BusinessService.ItemData>> add(
    @PathVariable String id,
    @Valid @RequestBody ItemRequest r
  ) {
    return ResponseEntity.status(201).body(
      ApiResponse.success("Content created", service.createItem(id, r))
    );
  }

  @PatchMapping("/private/{id}/items/{itemId}")
  public ResponseEntity<ApiResponse<BusinessService.ItemData>> edit(
    @PathVariable String id,
    @PathVariable String itemId,
    @Valid @RequestBody ItemRequest r
  ) {
    return ResponseEntity.ok(
      ApiResponse.success("Content updated", service.updateItem(id, itemId, r))
    );
  }

  @DeleteMapping("/private/{id}/items/{itemId}")
  public ResponseEntity<ApiResponse<Void>> remove(
    @PathVariable String id,
    @PathVariable String itemId
  ) {
    service.deleteItem(id, itemId);
    return ResponseEntity.ok(ApiResponse.success("Content removed"));
  }

  @GetMapping("/public/{slug}/items")
  public ResponseEntity<
    ApiResponse<PaginatedData<BusinessService.ItemData>>
  > publicItems(
    @PathVariable String slug,
    @RequestParam BusinessItem.Type type,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Content fetched",
        service.listItems(slug, type, page, size, true)
      )
    );
  }

  @PostMapping("/public/{slug}/orders")
  public ResponseEntity<ApiResponse<BusinessService.OrderData>> order(
    @PathVariable String slug,
    @Valid @RequestBody OrderRequest r
  ) {
    return ResponseEntity.status(201).body(
      ApiResponse.success("Order received", service.placeOrder(slug, r))
    );
  }

  @GetMapping("/private/{id}/orders")
  public ResponseEntity<
    ApiResponse<PaginatedData<BusinessService.OrderData>>
  > orders(
    @PathVariable String id,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success("Orders fetched", service.orders(id, page, size))
    );
  }

  @PatchMapping("/private/{id}/orders/{orderId}")
  public ResponseEntity<ApiResponse<BusinessService.OrderData>> orderStatus(
    @PathVariable String id,
    @PathVariable String orderId,
    @Valid @RequestBody StatusRequest r
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Order updated",
        service.orderStatus(
          id,
          orderId,
          BusinessOrder.Status.valueOf(r.status())
        )
      )
    );
  }

  @PostMapping("/public/{slug}/enquiries")
  public ResponseEntity<ApiResponse<BusinessService.EnquiryData>> enquire(
    @PathVariable String slug,
    @Valid @RequestBody EnquiryRequest r
  ) {
    return ResponseEntity.status(201).body(
      ApiResponse.success("Enquiry received", service.enquire(slug, r))
    );
  }

  @GetMapping("/private/{id}/enquiries")
  public ResponseEntity<
    ApiResponse<PaginatedData<BusinessService.EnquiryData>>
  > enquiries(
    @PathVariable String id,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Enquiries fetched",
        service.enquiries(id, page, size)
      )
    );
  }

  @PatchMapping("/private/{id}/enquiries/{enquiryId}")
  public ResponseEntity<ApiResponse<BusinessService.EnquiryData>> enquiryStatus(
    @PathVariable String id,
    @PathVariable String enquiryId,
    @Valid @RequestBody StatusRequest r
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Enquiry updated",
        service.enquiryStatus(
          id,
          enquiryId,
          BusinessEnquiry.Status.valueOf(r.status())
        )
      )
    );
  }
}
