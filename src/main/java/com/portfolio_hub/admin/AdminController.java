package com.portfolio_hub.admin;

import com.portfolio_hub.admin.request.AccountStatusRequest;
import com.portfolio_hub.admin.request.AdminAccountCreateRequest;
import com.portfolio_hub.admin.request.AdminActivityDeleteRequest;
import com.portfolio_hub.admin.request.AdminActivityRequest;
import com.portfolio_hub.admin.request.AdminUserUpdateRequest;
import com.portfolio_hub.admin.request.PortfolioImportMode;
import com.portfolio_hub.admin.response.AdminActivityResponse;
import com.portfolio_hub.admin.response.AdminAnalyticsResponse;
import com.portfolio_hub.admin.response.AdminUserDetailResponse;
import com.portfolio_hub.admin.response.PortfolioSetupPreview;
import com.portfolio_hub.admin.response.PortfolioSetupResult;
import com.portfolio_hub.analytics.PortfolioEvent;
import com.portfolio_hub.business.Business;
import com.portfolio_hub.business.BusinessEnquiry;
import com.portfolio_hub.business.BusinessItem;
import com.portfolio_hub.business.BusinessOrder;
import com.portfolio_hub.enquiry.response.EnquiryResponse;
import com.portfolio_hub.profile.response.ProfileEntryResponse;
import com.portfolio_hub.profile.response.SkillResponse;
import com.portfolio_hub.profile.response.SocialLinkResponse;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.response.UserResponse;
import com.portfolio_hub.utils.ApiResponse;
import com.portfolio_hub.utils.PaginatedData;
import com.portfolio_hub.utils.fileupload.ManagedFile;
import com.portfolio_hub.work.response.WorkResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

  private final AdminService adminService;
  private final PortfolioSetupService portfolioSetupService;

  @PostMapping("/private/portfolio-setup/accounts")
  public ResponseEntity<ApiResponse<UserResponse>> createAccount(
    @Valid @RequestBody AdminAccountCreateRequest request
  ) {
    return ResponseEntity.status(201).body(
      ApiResponse.success(
        "Account created. Password setup and email verification links have been sent to the user",
        portfolioSetupService.createAccount(request)
      )
    );
  }

  @PostMapping(
    value = "/private/portfolio-setup/preview",
    consumes = "multipart/form-data"
  )
  public ResponseEntity<
    ApiResponse<PortfolioSetupPreview>
  > previewPortfolioSetup(
    @RequestPart("file") MultipartFile file,
    @RequestParam(required = false) String userId
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Workbook checked successfully",
        portfolioSetupService.preview(file, userId)
      )
    );
  }

  @PostMapping(
    value = "/private/portfolio-setup/import",
    consumes = "multipart/form-data"
  )
  public ResponseEntity<ApiResponse<PortfolioSetupResult>> importPortfolioSetup(
    @RequestPart("file") MultipartFile file,
    @RequestParam(required = false) String userId,
    @RequestParam(defaultValue = "FILL_EMPTY") PortfolioImportMode mode
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Portfolio setup completed successfully",
        portfolioSetupService.apply(file, userId, mode)
      )
    );
  }

  @GetMapping("/private/users")
  public ResponseEntity<ApiResponse<PaginatedData<UserResponse>>> users(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String search,
    @RequestParam(required = false) User.AccountStatus status,
    @RequestParam(required = false) Boolean verified,
    @RequestParam(required = false) User.UserRole role,
    @RequestParam(defaultValue = "true") boolean includeSuperAdmins
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Users fetched successfully",
        adminService.users(
          page,
          size,
          search,
          status,
          verified,
          role,
          includeSuperAdmins
        )
      )
    );
  }

  @GetMapping("/private/users/{userId}")
  public ResponseEntity<ApiResponse<AdminUserDetailResponse>> user(
    @PathVariable String userId
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "User fetched successfully",
        adminService.user(userId)
      )
    );
  }

  @GetMapping("/private/users/{userId}/projects")
  public ResponseEntity<ApiResponse<PaginatedData<WorkResponse>>> projects(
    @PathVariable String userId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "User projects fetched",
        adminService.projects(userId, page, size)
      )
    );
  }

  @GetMapping("/private/users/{userId}/background")
  public ResponseEntity<
    ApiResponse<PaginatedData<ProfileEntryResponse>>
  > background(
    @PathVariable String userId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "User background fetched",
        adminService.entries(userId, page, size)
      )
    );
  }

  @GetMapping("/private/users/{userId}/skills")
  public ResponseEntity<ApiResponse<PaginatedData<SkillResponse>>> skills(
    @PathVariable String userId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "User skills fetched",
        adminService.skills(userId, page, size)
      )
    );
  }

  @GetMapping("/private/users/{userId}/social-links")
  public ResponseEntity<
    ApiResponse<PaginatedData<SocialLinkResponse>>
  > socialLinks(
    @PathVariable String userId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "User social links fetched",
        adminService.socialLinks(userId, page, size)
      )
    );
  }

  @GetMapping("/private/users/{userId}/enquiries")
  public ResponseEntity<ApiResponse<PaginatedData<EnquiryResponse>>> enquiries(
    @PathVariable String userId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "User enquiries fetched",
        adminService.enquiries(userId, page, size)
      )
    );
  }

  @GetMapping("/private/users/{userId}/analytics-events")
  public ResponseEntity<
    ApiResponse<PaginatedData<PortfolioEvent>>
  > analyticsEvents(
    @PathVariable String userId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "User analytics events fetched",
        adminService.analyticsEvents(userId, page, size)
      )
    );
  }

  @GetMapping("/private/users/{userId}/businesses")
  public ResponseEntity<ApiResponse<PaginatedData<Business>>> businesses(
    @PathVariable String userId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "User businesses fetched",
        adminService.businesses(userId, page, size)
      )
    );
  }

  @GetMapping("/private/users/{userId}/files")
  public ResponseEntity<ApiResponse<PaginatedData<ManagedFile>>> files(
    @PathVariable String userId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "User files fetched",
        adminService.files(userId, page, size)
      )
    );
  }

  @GetMapping("/private/users/{userId}/business-content")
  public ResponseEntity<
    ApiResponse<PaginatedData<BusinessItem>>
  > businessContent(
    @PathVariable String userId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Business content fetched",
        adminService.businessContent(userId, page, size)
      )
    );
  }

  @GetMapping("/private/users/{userId}/business-orders")
  public ResponseEntity<
    ApiResponse<PaginatedData<BusinessOrder>>
  > businessOrders(
    @PathVariable String userId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Business orders fetched",
        adminService.businessOrders(userId, page, size)
      )
    );
  }

  @GetMapping("/private/users/{userId}/business-enquiries")
  public ResponseEntity<
    ApiResponse<PaginatedData<BusinessEnquiry>>
  > businessEnquiries(
    @PathVariable String userId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Business enquiries fetched",
        adminService.businessEnquiries(userId, page, size)
      )
    );
  }

  @GetMapping("/private/users/{userId}/activity")
  public ResponseEntity<
    ApiResponse<PaginatedData<AdminActivityResponse>>
  > userActivity(
    @PathVariable String userId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "User activity fetched",
        adminService.userActivity(userId, page, size)
      )
    );
  }

  @GetMapping("/private/activity")
  public ResponseEntity<
    ApiResponse<PaginatedData<AdminActivityResponse>>
  > activity(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Account activity fetched",
        adminService.activity(page, size)
      )
    );
  }

  @PostMapping("/private/activity")
  public ResponseEntity<ApiResponse<AdminActivityResponse>> createActivity(
    @Valid @RequestBody AdminActivityRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Activity created",
        adminService.createActivity(request)
      )
    );
  }

  @PatchMapping("/private/activity/{activityId}")
  public ResponseEntity<ApiResponse<AdminActivityResponse>> updateActivity(
    @PathVariable String activityId,
    @Valid @RequestBody AdminActivityRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Activity updated",
        adminService.updateActivity(activityId, request)
      )
    );
  }

  @DeleteMapping("/private/activity/{activityId}")
  public ResponseEntity<ApiResponse<Void>> deleteActivity(
    @PathVariable String activityId
  ) {
    adminService.permanentlyDeleteActivity(activityId);
    return ResponseEntity.ok(
      ApiResponse.success("Activity permanently deleted")
    );
  }

  @PostMapping("/private/activity/hard-delete")
  public ResponseEntity<ApiResponse<Integer>> deleteActivities(
    @Valid @RequestBody AdminActivityDeleteRequest request
  ) {
    int deleted = adminService.permanentlyDeleteActivities(request.ids());
    return ResponseEntity.ok(
      ApiResponse.success("Activities permanently deleted", deleted)
    );
  }

  @PatchMapping("/private/users/{userId}")
  public ResponseEntity<ApiResponse<UserResponse>> updateUser(
    @PathVariable String userId,
    @Valid @RequestBody AdminUserUpdateRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "User information updated successfully",
        adminService.updateUser(userId, request)
      )
    );
  }

  @PatchMapping("/private/users/{userId}/status")
  public ResponseEntity<ApiResponse<UserResponse>> status(
    @PathVariable String userId,
    @Valid @RequestBody AccountStatusRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Account status updated successfully",
        adminService.changeStatus(userId, request)
      )
    );
  }

  @DeleteMapping("/private/users/{userId}")
  public ResponseEntity<ApiResponse<Void>> deleteUser(
    @PathVariable String userId
  ) {
    adminService.permanentlyDeleteUser(userId);
    return ResponseEntity.ok(
      ApiResponse.success("User and all associated data permanently deleted")
    );
  }

  @GetMapping("/private/analytics")
  public ResponseEntity<ApiResponse<AdminAnalyticsResponse>> analytics() {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Administration analytics fetched",
        adminService.analytics()
      )
    );
  }
}
