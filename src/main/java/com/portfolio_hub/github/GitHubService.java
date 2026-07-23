package com.portfolio_hub.github;

import com.portfolio_hub.utils.exception.InvalidInputException;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class GitHubService {

  private final ObjectMapper objectMapper;

  @Value("${application.github.token:}")
  private String token;

  public List<GitHubRepositoryResponse> repositories(String username) {
    if (
      username == null || !username.matches("^[A-Za-z0-9-]+$")
    ) throw new InvalidInputException("Invalid GitHub username");
    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder(
        URI.create(
          "https://api.github.com/users/" +
            username +
            "/repos?sort=updated&per_page=6&type=owner"
        )
      )
        .timeout(Duration.ofSeconds(8))
        .header("Accept", "application/vnd.github+json")
        .header("User-Agent", "Portfolio-Hub");
      if (token != null && !token.isBlank()) builder.header(
        "Authorization",
        "Bearer " + token.trim()
      );
      HttpResponse<String> response = HttpClient.newHttpClient().send(
        builder.GET().build(),
        HttpResponse.BodyHandlers.ofString()
      );
      if (response.statusCode() == 404) throw new ResourceNotFoundException(
        "GitHub user not found"
      );
      if (
        response.statusCode() < 200 || response.statusCode() >= 300
      ) return List.of();
      var root = objectMapper.readTree(response.body());
      List<GitHubRepositoryResponse> result = new ArrayList<>();
      root.forEach(node ->
        result.add(
          new GitHubRepositoryResponse(
            node.path("name").asText(),
            text(node, "description"),
            node.path("html_url").asText(),
            text(node, "language"),
            node.path("stargazers_count").asInt(),
            node.path("forks_count").asInt(),
            node.path("updated_at").asText()
          )
        )
      );
      return result;
    } catch (ResourceNotFoundException exception) {
      throw exception;
    } catch (Exception exception) {
      return List.of();
    }
  }

  private String text(JsonNode node, String field) {
    return node.path(field).isNull() ? null : node.path(field).asText();
  }
}
