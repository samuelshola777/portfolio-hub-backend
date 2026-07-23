package com.portfolio_hub.profile;

import com.portfolio_hub.profile.request.ProfileEntryRequest;
import com.portfolio_hub.profile.request.SkillRequest;
import com.portfolio_hub.profile.request.SocialLinkRequest;
import com.portfolio_hub.profile.response.ProfileContentResponse;
import com.portfolio_hub.profile.response.ProfileEntryResponse;
import com.portfolio_hub.profile.response.SkillResponse;
import com.portfolio_hub.profile.response.SocialLinkResponse;
import com.portfolio_hub.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile-content/private")
@RequiredArgsConstructor
public class ProfileContentController {

  private final ProfileContentService service;

  @GetMapping
  public ResponseEntity<ApiResponse<ProfileContentResponse>> mine() {
    return ResponseEntity.ok(
      ApiResponse.success("Profile sections fetched", service.mine())
    );
  }

  @PostMapping("/entries")
  public ResponseEntity<ApiResponse<ProfileEntryResponse>> createEntry(
    @Valid @RequestBody ProfileEntryRequest request
  ) {
    return ResponseEntity.status(201).body(
      ApiResponse.success("Profile entry created", service.createEntry(request))
    );
  }

  @PutMapping("/entries/{id}")
  public ResponseEntity<ApiResponse<ProfileEntryResponse>> updateEntry(
    @PathVariable String id,
    @Valid @RequestBody ProfileEntryRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Profile entry updated",
        service.updateEntry(id, request)
      )
    );
  }

  @DeleteMapping("/entries/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteEntry(
    @PathVariable String id
  ) {
    service.deleteEntry(id);
    return ResponseEntity.ok(ApiResponse.success("Profile entry removed"));
  }

  @PostMapping("/skills")
  public ResponseEntity<ApiResponse<SkillResponse>> createSkill(
    @Valid @RequestBody SkillRequest request
  ) {
    return ResponseEntity.status(201).body(
      ApiResponse.success("Skill created", service.createSkill(request))
    );
  }

  @PutMapping("/skills/{id}")
  public ResponseEntity<ApiResponse<SkillResponse>> updateSkill(
    @PathVariable String id,
    @Valid @RequestBody SkillRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success("Skill updated", service.updateSkill(id, request))
    );
  }

  @DeleteMapping("/skills/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteSkill(
    @PathVariable String id
  ) {
    service.deleteSkill(id);
    return ResponseEntity.ok(ApiResponse.success("Skill removed"));
  }

  @PostMapping("/social-links")
  public ResponseEntity<ApiResponse<SocialLinkResponse>> createSocial(
    @Valid @RequestBody SocialLinkRequest request
  ) {
    return ResponseEntity.status(201).body(
      ApiResponse.success("Social link created", service.createSocial(request))
    );
  }

  @PutMapping("/social-links/{id}")
  public ResponseEntity<ApiResponse<SocialLinkResponse>> updateSocial(
    @PathVariable String id,
    @Valid @RequestBody SocialLinkRequest request
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "Social link updated",
        service.updateSocial(id, request)
      )
    );
  }

  @DeleteMapping("/social-links/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteSocial(
    @PathVariable String id
  ) {
    service.deleteSocial(id);
    return ResponseEntity.ok(ApiResponse.success("Social link removed"));
  }
}
