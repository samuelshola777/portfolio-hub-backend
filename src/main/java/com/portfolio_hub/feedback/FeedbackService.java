package com.portfolio_hub.feedback;

import com.portfolio_hub.announcement.Announcement;
import com.portfolio_hub.announcement.AnnouncementEmailDispatcher;
import com.portfolio_hub.announcement.AnnouncementRecipient;
import com.portfolio_hub.announcement.AnnouncementRecipientRepository;
import com.portfolio_hub.announcement.AnnouncementRepository;
import com.portfolio_hub.feedback.request.FeedbackCreateRequest;
import com.portfolio_hub.feedback.request.FeedbackResponseRequest;
import com.portfolio_hub.feedback.response.FeedbackResponse;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.utils.PaginatedData;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import com.portfolio_hub.utils.exception.UnauthorizedException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

  private final FeedbackRepository feedbackRepository;
  private final UserRepository userRepository;
  private final AnnouncementRepository announcementRepository;
  private final AnnouncementRecipientRepository recipientRepository;
  private final AnnouncementEmailDispatcher emailDispatcher;

  @Transactional
  public FeedbackResponse create(FeedbackCreateRequest request) {
    User user = currentUser();
    return map(
      feedbackRepository.save(
        Feedback.builder()
          .ownerId(user.getId())
          .category(request.category())
          .subject(request.subject().trim())
          .message(request.message().trim())
          .status(Feedback.Status.OPEN)
          .build()
      )
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<FeedbackResponse> mine(int page, int size) {
    User user = currentUser();
    return PaginatedData.from(
      feedbackRepository.findByOwnerIdOrderByCreatedAtDesc(
        user.getId(),
        page(page, size)
      ),
      this::map
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<FeedbackResponse> all(
    int page,
    int size,
    Feedback.Status status
  ) {
    currentUser();
    return PaginatedData.from(
      status == null
        ? feedbackRepository.findAllByOrderByCreatedAtDesc(page(page, size))
        : feedbackRepository.findByStatusOrderByCreatedAtDesc(
          status,
          page(page, size)
        ),
      this::map
    );
  }

  @Transactional
  public FeedbackResponse respond(String id, FeedbackResponseRequest request) {
    User admin = currentUser();
    Feedback ticket = feedbackRepository
      .findById(id)
      .orElseThrow(() ->
        new ResourceNotFoundException("Feedback ticket not found")
      );
    User owner = userRepository
      .findById(ticket.getOwnerId())
      .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    ticket.setAdminResponse(request.response().trim());
    ticket.setStatus(Feedback.Status.RESPONDED);
    ticket.setRespondedById(admin.getId());
    ticket.setRespondedAt(LocalDateTime.now());
    feedbackRepository.flush();

    String subject =
      "Response to your " +
      ticket.getCategory().name().toLowerCase().replace('_', ' ') +
      ": " +
      ticket.getSubject();
    Announcement announcement = announcementRepository.save(
      Announcement.builder()
        .createdById(admin.getId())
        .subject(subject)
        .message(ticket.getAdminResponse())
        .attachmentsJson("[]")
        .build()
    );
    recipientRepository.save(
      AnnouncementRecipient.builder()
        .announcementId(announcement.getId())
        .userId(owner.getId())
        .build()
    );
    emailDispatcher.send(
      List.of(owner),
      subject,
      ticket.getAdminResponse(),
      List.of()
    );
    return map(ticket);
  }

  private PageRequest page(int page, int size) {
    return PageRequest.of(
      Math.max(0, page - 1),
      Math.min(100, Math.max(1, size))
    );
  }

  private User currentUser() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (
      auth == null || !auth.isAuthenticated()
    ) throw new UnauthorizedException("Authentication required");
    return userRepository
      .findByEmailAddressIgnoreCaseAndDeletedFalse(auth.getName())
      .orElseThrow(() -> new UnauthorizedException("Authentication required"));
  }

  private FeedbackResponse map(Feedback ticket) {
    User owner = userRepository.findById(ticket.getOwnerId()).orElse(null);
    return new FeedbackResponse(
      ticket.getId(),
      ticket.getOwnerId(),
      owner == null ? "Deleted user" : owner.getFullName(),
      owner == null ? "" : owner.getEmailAddress(),
      ticket.getCategory(),
      ticket.getSubject(),
      ticket.getMessage(),
      ticket.getStatus(),
      ticket.getAdminResponse(),
      ticket.getCreatedAt(),
      ticket.getRespondedAt()
    );
  }
}
