package com.portfolio_hub.enquiry;

import com.portfolio_hub.analytics.PortfolioEvent;
import com.portfolio_hub.analytics.PortfolioEventRepository;
import com.portfolio_hub.enquiry.request.EnquiryRequest;
import com.portfolio_hub.enquiry.response.EnquiryResponse;
import com.portfolio_hub.portfolio.Portfolio;
import com.portfolio_hub.portfolio.PortfolioRepository;
import com.portfolio_hub.portfolio.PortfolioService;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.utils.PaginatedData;
import com.portfolio_hub.utils.emailsenderservice.EmailSenderService;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnquiryService {

  private final EnquiryRepository enquiryRepository;
  private final PortfolioRepository portfolioRepository;
  private final PortfolioService portfolioService;
  private final PortfolioEventRepository eventRepository;
  private final UserRepository userRepository;
  private final EmailSenderService emailSenderService;

  @Value("${application.front-end-url}")
  private String frontEndUrl;

  @Transactional
  public EnquiryResponse create(
    String username,
    EnquiryRequest request,
    HttpServletRequest http
  ) {
    Portfolio portfolio = portfolioRepository
      .findByUsernameIgnoreCaseAndStatus(
        username,
        Portfolio.PublicationStatus.PUBLISHED
      )
      .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));

    Enquiry enquiry = enquiryRepository.save(
      Enquiry.builder()
        .ownerId(portfolio.getOwnerId())
        .portfolioId(portfolio.getId())
        .recruiterName(request.name().trim())
        .recruiterEmail(request.email().trim().toLowerCase())
        .company(clean(request.company()))
        .message(request.message().trim())
        .status(Enquiry.Status.NEW)
        .build()
    );

    eventRepository.save(
      PortfolioEvent.builder()
        .ownerId(portfolio.getOwnerId())
        .portfolioId(portfolio.getId())
        .eventType(PortfolioEvent.EventType.ENQUIRY)
        .source(clean(http.getHeader("Referer")))
        .country(first(http, "CF-IPCountry", "X-Vercel-IP-Country"))
        .city(first(http, "X-Vercel-IP-City", "X-City"))
        .build()
    );

    userRepository
      .findById(portfolio.getOwnerId())
      .filter(user -> !user.isDeleted())
      .ifPresent(owner -> sendOwnerNotification(owner, enquiry));

    return map(enquiry);
  }

  public List<EnquiryResponse> mine() {
    Portfolio portfolio = portfolioService.findMine();

    return enquiryRepository
      .findByOwnerIdOrderByCreatedAtDesc(portfolio.getOwnerId())
      .stream()
      .map(EnquiryService::map)
      .toList();
  }

  public PaginatedData<EnquiryResponse> pageMine(int page, int size) {
    Portfolio portfolio = portfolioService.findMine();
    return PaginatedData.from(
      enquiryRepository.findByOwnerId(
        portfolio.getOwnerId(),
        PageRequest.of(
          Math.max(0, page - 1),
          Math.min(100, Math.max(1, size)),
          Sort.by(Sort.Direction.DESC, "createdAt")
        )
      ),
      EnquiryService::map
    );
  }

  public EnquiryResponse status(String id, Enquiry.Status status) {
    Portfolio portfolio = portfolioService.findMine();

    Enquiry enquiry = enquiryRepository
      .findByIdAndOwnerId(id, portfolio.getOwnerId())
      .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found"));

    enquiry.setStatus(status);

    return map(enquiryRepository.save(enquiry));
  }

  private void sendOwnerNotification(User owner, Enquiry enquiry) {
    emailSenderService.sendNewEnquiryNotification(
      owner.getEmailAddress(),
      owner.getFullName(),
      enquiry.getRecruiterName(),
      enquiry.getRecruiterEmail(),
      enquiry.getCompany(),
      enquiry.getMessage(),
      frontEndUrl + "/dashboard?tab=Enquiries"
    );
  }

  private static EnquiryResponse map(Enquiry enquiry) {
    return new EnquiryResponse(
      enquiry.getId(),
      enquiry.getRecruiterName(),
      enquiry.getRecruiterEmail(),
      enquiry.getCompany(),
      enquiry.getMessage(),
      enquiry.getStatus(),
      enquiry.getCreatedAt()
    );
  }

  private String first(HttpServletRequest request, String... names) {
    for (String name : names) {
      String value = clean(request.getHeader(name));

      if (value != null) {
        return value;
      }
    }

    return "Unknown";
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
