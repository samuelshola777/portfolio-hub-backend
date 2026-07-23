package com.portfolio_hub.analytics;

import com.portfolio_hub.analytics.request.AnalyticsEventRequest;
import com.portfolio_hub.analytics.response.AnalyticsSummaryResponse;
import com.portfolio_hub.portfolio.Portfolio;
import com.portfolio_hub.portfolio.PortfolioRepository;
import com.portfolio_hub.portfolio.PortfolioService;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import com.portfolio_hub.work.WorkRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

  private final PortfolioEventRepository eventRepository;
  private final PortfolioRepository portfolioRepository;
  private final PortfolioService portfolioService;
  private final WorkRepository workRepository;

  public void record(
    String username,
    AnalyticsEventRequest request,
    HttpServletRequest http
  ) {
    Portfolio portfolio = portfolioRepository
      .findByUsernameIgnoreCaseAndStatus(
        username,
        Portfolio.PublicationStatus.PUBLISHED
      )
      .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
    eventRepository.save(
      PortfolioEvent.builder()
        .ownerId(portfolio.getOwnerId())
        .portfolioId(portfolio.getId())
        .eventType(request.eventType())
        .targetId(clean(request.targetId()))
        .source(resolveSource(request.source(), http.getHeader("Referer")))
        .country(
          header(http, "CF-IPCountry", "X-Vercel-IP-Country", "X-Country")
        )
        .city(header(http, "X-Vercel-IP-City", "X-City"))
        .build()
    );
  }

  public AnalyticsSummaryResponse mine(int days) {
    Portfolio portfolio = portfolioService.findMine();
    int range = Math.min(365, Math.max(7, days));
    var events =
      eventRepository.findByOwnerIdAndCreatedAtAfterOrderByCreatedAtAsc(
        portfolio.getOwnerId(),
        LocalDateTime.now().minusDays(range)
      );
    Map<LocalDate, Long> daily = new LinkedHashMap<>();
    for (int i = range - 1; i >= 0; i--) daily.put(
      LocalDate.now().minusDays(i),
      0L
    );
    events
      .stream()
      .filter(e -> e.getEventType() == PortfolioEvent.EventType.VIEW)
      .forEach(e ->
        daily.computeIfPresent(
          e.getCreatedAt().toLocalDate(),
          (key, value) -> value + 1
        )
      );

    Map<String, String> projectNames = workRepository
      .findAllById(
        events
          .stream()
          .map(PortfolioEvent::getTargetId)
          .filter(v -> v != null)
          .toList()
      )
      .stream()
      .collect(
        Collectors.toMap(
          work -> work.getId(),
          work -> work.getTitle(),
          (a, b) -> a
        )
      );
    return new AnalyticsSummaryResponse(
      count(events, PortfolioEvent.EventType.VIEW),
      count(events, PortfolioEvent.EventType.PROJECT_CLICK),
      count(events, PortfolioEvent.EventType.CV_DOWNLOAD),
      count(events, PortfolioEvent.EventType.SOCIAL_CLICK),
      count(events, PortfolioEvent.EventType.WEBSITE_CLICK),
      count(events, PortfolioEvent.EventType.EXPORT_DOWNLOAD),
      count(events, PortfolioEvent.EventType.ENQUIRY),
      daily
        .entrySet()
        .stream()
        .map(e ->
          new AnalyticsSummaryResponse.DailyMetric(e.getKey(), e.getValue())
        )
        .toList(),
      top(events, PortfolioEvent::getSource),
      top(events, event -> location(event.getCountry(), event.getCity())),
      top(events
        .stream()
        .filter(e -> e.getEventType() == PortfolioEvent.EventType.PROJECT_CLICK)
        .toList(), e -> projectNames.getOrDefault(e.getTargetId(), "Project"))
    );
  }

  private long count(
    java.util.List<PortfolioEvent> events,
    PortfolioEvent.EventType type
  ) {
    return events
      .stream()
      .filter(e -> e.getEventType() == type)
      .count();
  }

  private java.util.List<AnalyticsSummaryResponse.NameCount> top(
    java.util.List<PortfolioEvent> events,
    Function<PortfolioEvent, String> classifier
  ) {
    return events
      .stream()
      .map(classifier)
      .filter(value -> value != null && !value.isBlank())
      .collect(
        Collectors.groupingBy(Function.identity(), Collectors.counting())
      )
      .entrySet()
      .stream()
      .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
      .limit(8)
      .map(e ->
        new AnalyticsSummaryResponse.NameCount(e.getKey(), e.getValue())
      )
      .toList();
  }

  private String resolveSource(String supplied, String referer) {
    String value = clean(supplied) != null ? clean(supplied) : clean(referer);
    if (value == null) return "Direct";
    try {
      URI uri = URI.create(value);
      return uri.getHost() == null ? value : uri.getHost();
    } catch (Exception ignored) {
      return value;
    }
  }

  private String location(String country, String city) {
    return country == null
      ? "Unknown"
      : city == null
        ? country
        : city + ", " + country;
  }

  private String header(HttpServletRequest request, String... names) {
    for (String name : names) {
      String value = clean(request.getHeader(name));
      if (value != null) return value;
    }
    return "Unknown";
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
