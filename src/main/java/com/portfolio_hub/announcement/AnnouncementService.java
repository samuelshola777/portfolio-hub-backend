package com.portfolio_hub.announcement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio_hub.announcement.request.AnnouncementRequest;
import com.portfolio_hub.announcement.response.AnnouncementResponse;
import com.portfolio_hub.announcement.response.AnnouncementSendResponse;
import com.portfolio_hub.userauthmgt.user.AdminUserSpecifications;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.userauthmgt.user.UserService;
import com.portfolio_hub.utils.PaginatedData;
import com.portfolio_hub.utils.exception.InvalidOperationException;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import com.portfolio_hub.utils.fileupload.FileUploadCategory;
import com.portfolio_hub.utils.fileupload.ManagedFile;
import com.portfolio_hub.utils.fileupload.ManagedFileRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

  private final AnnouncementRepository announcementRepository;
  private final AnnouncementRecipientRepository recipientRepository;
  private final UserRepository userRepository;
  private final UserService userService;
  private final AnnouncementEmailDispatcher emailDispatcher;
  private final ObjectMapper objectMapper;
  private final ManagedFileRepository managedFileRepository;

  @Transactional
  public AnnouncementSendResponse send(AnnouncementRequest request) {
    User actor = userService.currentUser();
    List<User> recipients = resolveRecipients(request);
    if (recipients.isEmpty()) throw new InvalidOperationException(
      "Select at least one available user"
    );
    List<AnnouncementRequest.Attachment> attachments = validatedAttachments(
      actor,
      request.attachments()
    );

    Announcement announcement = announcementRepository.save(
      Announcement.builder()
        .createdById(actor.getId())
        .subject(request.subject().trim())
        .message(request.message().trim())
        .attachmentsJson(writeAttachments(attachments))
        .build()
    );
    recipientRepository.saveAll(
      recipients
        .stream()
        .map(user ->
          AnnouncementRecipient.builder()
            .announcementId(announcement.getId())
            .userId(user.getId())
            .build()
        )
        .toList()
    );
    emailDispatcher.send(
      recipients,
      announcement.getSubject(),
      announcement.getMessage(),
      attachments
    );
    return new AnnouncementSendResponse(
      announcement.getId(),
      recipients.size()
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<AnnouncementResponse> mine(int page, int size) {
    User user = userService.currentUser();
    return PaginatedData.from(
      recipientRepository.findByUserIdOrderByCreatedAtDesc(
        user.getId(),
        PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)))
      ),
      this::map
    );
  }

  @Transactional
  public AnnouncementResponse markRead(String recipientId) {
    User user = userService.currentUser();
    AnnouncementRecipient recipient = recipientRepository
      .findByIdAndUserId(recipientId, user.getId())
      .orElseThrow(() ->
        new ResourceNotFoundException("Announcement not found")
      );
    if (recipient.getReadAt() == null) recipient.setReadAt(LocalDateTime.now());
    return map(recipientRepository.save(recipient));
  }

  private AnnouncementResponse map(AnnouncementRecipient recipient) {
    Announcement announcement = announcementRepository
      .findById(recipient.getAnnouncementId())
      .orElseThrow(() ->
        new ResourceNotFoundException("Announcement not found")
      );
    return new AnnouncementResponse(
      recipient.getId(),
      announcement.getId(),
      announcement.getSubject(),
      announcement.getMessage(),
      readAttachments(announcement.getAttachmentsJson()),
      announcement.getCreatedAt(),
      recipient.getReadAt()
    );
  }

  private String writeAttachments(
    List<AnnouncementRequest.Attachment> attachments
  ) {
    try {
      return objectMapper.writeValueAsString(safeAttachments(attachments));
    } catch (JsonProcessingException exception) {
      throw new InvalidOperationException(
        "Unable to save announcement attachments"
      );
    }
  }

  private List<AnnouncementRequest.Attachment> readAttachments(String value) {
    if (value == null || value.isBlank()) return List.of();
    try {
      return objectMapper.readValue(value, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      return List.of();
    }
  }

  private List<AnnouncementRequest.Attachment> safeAttachments(
    List<AnnouncementRequest.Attachment> attachments
  ) {
    return attachments == null ? List.of() : attachments;
  }

  private List<AnnouncementRequest.Attachment> validatedAttachments(
    User actor,
    List<AnnouncementRequest.Attachment> requested
  ) {
    return safeAttachments(requested)
      .stream()
      .map(attachment -> {
        ManagedFile file = managedFileRepository
          .findByFileUrl(attachment.url())
          .filter(value -> actor.getId().equals(value.getOwnerId()))
          .orElseThrow(() ->
            new InvalidOperationException(
              "Every attachment must be a file uploaded by this administrator"
            )
          );
        return new AnnouncementRequest.Attachment(
          file.getFileUrl(),
          file.getOriginalFileName(),
          contentType(file),
          file.getFileSizeBytes()
        );
      })
      .toList();
  }

  private String contentType(ManagedFile file) {
    String format = file.getFormat() == null
      ? ""
      : file.getFormat().toLowerCase();
    if (file.getCategory() == FileUploadCategory.IMAGE) return (
      "image/" + (format.equals("jpg") ? "jpeg" : format)
    );
    if (file.getCategory() == FileUploadCategory.VIDEO) return (
      "video/" + format
    );
    if (format.equals("pdf")) return "application/pdf";
    return "application/octet-stream";
  }

  private List<User> resolveRecipients(AnnouncementRequest request) {
    if (request.allMatching()) {
      var excluded = new LinkedHashSet<>(
        request.excludedUserIds() == null
          ? List.of()
          : request.excludedUserIds()
      );
      return userRepository
        .findAll(
          AdminUserSpecifications.matching(
            clean(request.recipientSearch()),
            request.recipientStatus(),
            request.recipientVerified(),
            request.recipientRole(),
            false
          ),
          Sort.by(Sort.Direction.DESC, "createdAt")
        )
        .stream()
        .filter(user -> !excluded.contains(user.getId()))
        .toList();
    }

    List<String> requestedIds = new LinkedHashSet<>(
      request.userIds() == null ? List.of() : request.userIds()
    )
      .stream()
      .toList();
    List<User> recipients = userRepository
      .findAllById(requestedIds)
      .stream()
      .filter(
        user -> !user.isDeleted() && user.getRole() != User.UserRole.SUPER_ADMIN
      )
      .toList();
    if (recipients.size() != requestedIds.size()) {
      throw new InvalidOperationException(
        "One or more selected users are unavailable"
      );
    }
    return recipients;
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
