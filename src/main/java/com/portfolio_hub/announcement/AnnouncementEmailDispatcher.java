package com.portfolio_hub.announcement;

import com.portfolio_hub.announcement.request.AnnouncementRequest;
import com.portfolio_hub.userauthmgt.user.User;
import jakarta.mail.internet.MimeMessage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementEmailDispatcher {

  private static final long MAX_EMAIL_ATTACHMENT_BYTES = 18L * 1024 * 1024;
  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;

  @Value(
    "${application.configuration.organization.sender-email:samuel@lightworkstechnologies.com}"
  )
  private String fromEmail;

  @Value("${application.configuration.organization.name:Portfolio Hub}")
  private String organizationName;

  @Value(
    "${application.configuration.organization.support-email:samuel@lightworkstechnologies.com}"
  )
  private String supportEmail;

  @Value("${application.front-end-url:http://localhost:3000}")
  private String frontEndUrl;

  @Async
  public void send(
    List<User> recipients,
    String subject,
    String message,
    List<AnnouncementRequest.Attachment> attachments
  ) {
    List<AnnouncementRequest.Attachment> safeAttachments = attachments == null
      ? List.of()
      : attachments;
    List<AttachmentView> views = safeAttachments
      .stream()
      .map(this::view)
      .toList();
    List<PreparedAttachment> prepared = safeAttachments
      .stream()
      .map(this::prepare)
      .filter(value -> value != null)
      .toList();

    Context context = new Context(Locale.getDefault());
    context.setVariable("subject", subject);
    context.setVariable("message", message);
    context.setVariable("attachments", views);
    context.setVariable("hasAttachments", !views.isEmpty());
    context.setVariable(
      "dashboardUrl",
      frontEndUrl.replaceAll("/$", "") + "/dashboard?tab=Announcements"
    );
    context.setVariable("organizationName", organizationName);
    context.setVariable("supportEmail", supportEmail);
    context.setVariable("year", Year.now().getValue());
    String html = templateEngine.process(
      "portfolio-hub-announcement-template",
      context
    );
    String plainText = plainText(message, safeAttachments);

    recipients.forEach(user ->
      sendOne(user.getEmailAddress(), subject, plainText, html, prepared)
    );
  }

  private void sendOne(
    String recipient,
    String subject,
    String plainText,
    String html,
    List<PreparedAttachment> attachments
  ) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(
        mimeMessage,
        true,
        "UTF-8"
      );
      helper.setFrom(fromEmail, organizationName);
      helper.setTo(recipient);
      helper.setSubject(subject);
      helper.setText(plainText, html);
      for (PreparedAttachment attachment : attachments) {
        helper.addAttachment(
          attachment.name(),
          new ByteArrayResource(attachment.bytes()),
          attachment.contentType()
        );
      }
      mailSender.send(mimeMessage);
      log.info(
        "Announcement email sent to {} with {} attachments",
        recipient,
        attachments.size()
      );
    } catch (Exception exception) {
      log.error(
        "Unable to send announcement email to {}: {}",
        recipient,
        exception.getMessage(),
        exception
      );
    }
  }

  private PreparedAttachment prepare(
    AnnouncementRequest.Attachment attachment
  ) {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(attachment.url()))
        .timeout(Duration.ofSeconds(25))
        .GET()
        .build();
      HttpResponse<byte[]> response = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
        .send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (
        response.statusCode() < 200 ||
        response.statusCode() >= 300 ||
        response.body().length > MAX_EMAIL_ATTACHMENT_BYTES
      ) {
        log.warn(
          "Skipping MIME attachment {} because it could not be downloaded safely",
          attachment.name()
        );
        return null;
      }
      String contentType = attachment.contentType() == null ||
        attachment.contentType().isBlank()
        ? response
          .headers()
          .firstValue("content-type")
          .orElse("application/octet-stream")
        : attachment.contentType();
      return new PreparedAttachment(
        attachment.name(),
        contentType,
        response.body()
      );
    } catch (Exception exception) {
      log.warn(
        "Unable to prepare announcement attachment {}: {}",
        attachment.name(),
        exception.getMessage()
      );
      return null;
    }
  }

  private AttachmentView view(AnnouncementRequest.Attachment attachment) {
    String type = attachment.contentType() == null
      ? ""
      : attachment.contentType().toLowerCase(Locale.ROOT);
    String name = attachment.name().toLowerCase(Locale.ROOT);
    boolean image =
      type.startsWith("image/") || name.matches(".*\\.(png|jpe?g|gif|webp)$");
    boolean video =
      type.startsWith("video/") || name.matches(".*\\.(mp4|webm|mov|m4v)$");
    return new AttachmentView(
      attachment.url(),
      attachment.name(),
      readableSize(attachment.size()),
      image,
      video,
      !image && !video
    );
  }

  private String plainText(
    String message,
    List<AnnouncementRequest.Attachment> attachments
  ) {
    StringBuilder value = new StringBuilder(message.trim())
      .append("\n\nOpen your dashboard: ")
      .append(frontEndUrl.replaceAll("/$", ""))
      .append("/dashboard?tab=Announcements");
    if (!attachments.isEmpty()) {
      value.append("\n\nAttachments included with this email:");
      attachments.forEach(attachment ->
        value.append("\n- ").append(attachment.name())
      );
    }
    return value.toString();
  }

  private String readableSize(Long bytes) {
    if (bytes == null || bytes <= 0) return "Attachment";
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return String.format(
      Locale.ROOT,
      "%.1f KB",
      bytes / 1024d
    );
    return String.format(Locale.ROOT, "%.1f MB", bytes / 1024d / 1024d);
  }

  public record AttachmentView(
    String url,
    String name,
    String size,
    boolean image,
    boolean video,
    boolean document
  ) {}

  private record PreparedAttachment(
    String name,
    String contentType,
    byte[] bytes
  ) {}
}
