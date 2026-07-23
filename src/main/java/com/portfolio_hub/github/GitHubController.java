package com.portfolio_hub.github;

import com.portfolio_hub.utils.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/github")
@RequiredArgsConstructor
public class GitHubController {

  private final GitHubService service;

  @GetMapping("/public/{username}/repositories")
  public ResponseEntity<
    ApiResponse<List<GitHubRepositoryResponse>>
  > repositories(@PathVariable String username) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "GitHub repositories fetched",
        service.repositories(username)
      )
    );
  }
}
