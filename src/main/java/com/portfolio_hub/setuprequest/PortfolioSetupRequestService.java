package com.portfolio_hub.setuprequest;

import com.portfolio_hub.business.Business;
import com.portfolio_hub.business.BusinessRepository;
import com.portfolio_hub.setuprequest.request.SetupAssistanceRequest;
import com.portfolio_hub.setuprequest.request.SetupRequestStatusUpdate;
import com.portfolio_hub.setuprequest.response.SetupRequestResponse;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserService;
import com.portfolio_hub.userauthmgt.user.WhatsAppNumber;
import com.portfolio_hub.utils.PaginatedData;
import com.portfolio_hub.utils.exception.InvalidOperationException;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortfolioSetupRequestService {

  private final PortfolioSetupRequestRepository repository;
  private final UserService userService;
  private final BusinessRepository businesses;

  @Transactional
  public SetupRequestResponse create(SetupAssistanceRequest request) {
    PortfolioSetupRequest setupRequest = PortfolioSetupRequest.builder()
      .fullName(request.fullName().trim())
      .email(request.email().trim().toLowerCase())
      .whatsAppNumber(WhatsAppNumber.normalize(request.whatsAppNumber()))
      .message(clean(request.message()))
      .status(PortfolioSetupRequest.Status.NEW)
      .targetType(PortfolioSetupRequest.TargetType.PORTFOLIO)
      .build();
    return map(repository.save(setupRequest));
  }

  @Transactional
  public SetupRequestResponse createForBusiness(
    String businessId,
    String message
  ) {
    User user = userService.currentUser();
    Business business = businesses
      .findByIdAndOwnerId(businessId, user.getId())
      .orElseThrow(() ->
        new ResourceNotFoundException("We could not find that business")
      );
    var activeStatuses = java.util.List.of(
      PortfolioSetupRequest.Status.NEW,
      PortfolioSetupRequest.Status.CONTACTED,
      PortfolioSetupRequest.Status.IN_PROGRESS
    );
    String whatsAppNumber = clean(user.getWhatsAppNumber()) == null
      ? clean(business.getPhone())
      : clean(user.getWhatsAppNumber());
    if (whatsAppNumber == null) {
      throw new InvalidOperationException(
        "Add the WhatsApp number you actively use before requesting assistance"
      );
    }
    return repository
      .findFirstByWorkspaceIdAndStatusInOrderByCreatedAtDesc(
        business.getId(),
        activeStatuses
      )
      .map(this::map)
      .orElseGet(() ->
        map(
          repository.save(
            PortfolioSetupRequest.builder()
              .fullName(user.getFullName())
              .email(user.getEmailAddress())
              .whatsAppNumber(WhatsAppNumber.normalize(whatsAppNumber))
              .message(clean(message))
              .status(PortfolioSetupRequest.Status.NEW)
              .targetType(PortfolioSetupRequest.TargetType.BUSINESS)
              .ownerId(user.getId())
              .workspaceId(business.getId())
              .build()
          )
        )
      );
  }

  @Transactional(readOnly = true)
  public PaginatedData<SetupRequestResponse> list(
    PortfolioSetupRequest.Status status,
    int page,
    int size
  ) {
    requireAdministrator();
    var pageable = PageRequest.of(
      Math.max(0, page - 1),
      Math.min(50, Math.max(1, size))
    );
    var result = status == null
      ? repository.findAllByOrderByCreatedAtDesc(pageable)
      : repository.findByStatusOrderByCreatedAtDesc(status, pageable);
    return PaginatedData.from(result, this::map);
  }

  @Transactional
  public SetupRequestResponse update(
    String id,
    SetupRequestStatusUpdate request
  ) {
    requireAdministrator();
    PortfolioSetupRequest setupRequest = repository
      .findById(id)
      .orElseThrow(() ->
        new ResourceNotFoundException("We could not find that setup request")
      );
    setupRequest.setStatus(request.status());
    setupRequest.setAdminNote(clean(request.adminNote()));
    return map(repository.save(setupRequest));
  }

  private void requireAdministrator() {
    User user = userService.currentUser();
    if (user.getRole() != User.UserRole.SUPER_ADMIN) {
      throw new InvalidOperationException("Administrator access is required");
    }
  }

  private SetupRequestResponse map(PortfolioSetupRequest value) {
    return new SetupRequestResponse(
      value.getId(),
      value.getFullName(),
      value.getEmail(),
      value.getWhatsAppNumber(),
      value.getMessage(),
      value.getStatus(),
      value.getAdminNote(),
      value.getTargetType() == null
        ? PortfolioSetupRequest.TargetType.PORTFOLIO
        : value.getTargetType(),
      value.getOwnerId(),
      value.getWorkspaceId(),
      value.getCreatedAt(),
      value.getUpdatedAt()
    );
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
