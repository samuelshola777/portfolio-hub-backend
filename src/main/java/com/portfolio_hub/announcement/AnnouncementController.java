package com.portfolio_hub.announcement;

import com.portfolio_hub.announcement.request.AnnouncementRequest;
import com.portfolio_hub.announcement.response.AnnouncementResponse;
import com.portfolio_hub.announcement.response.AnnouncementSendResponse;
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
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

  private final AnnouncementService announcementService;

  @PostMapping("/admin/private")
  public ResponseEntity<ApiResponse<AnnouncementSendResponse>> send(
    @Valid @RequestBody AnnouncementRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Announcement sent successfully",
        announcementService.send(request)
      )
    );
  }

  @GetMapping("/private/mine")
  public ResponseEntity<ApiResponse<PaginatedData<AnnouncementResponse>>> mine(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Announcements fetched",
        announcementService.mine(page, size)
      )
    );
  }

  @PatchMapping("/private/{recipientId}/read")
  public ResponseEntity<ApiResponse<AnnouncementResponse>> read(
    @PathVariable String recipientId
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Announcement marked as read",
        announcementService.markRead(recipientId)
      )
    );
  }
}
