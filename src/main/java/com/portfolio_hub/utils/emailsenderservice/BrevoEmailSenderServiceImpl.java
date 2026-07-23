package com.portfolio_hub.utils.emailsenderservice;

import jakarta.mail.internet.MimeMessage;
import java.time.Year;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrevoEmailSenderServiceImpl implements EmailSenderService {

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

  @Value("${app.verification.expiration-hours:24}")
  private long verificationExpirationHours;

  @Value("${app.password-reset.expiration-hours:1}")
  private long passwordResetExpirationHours;

  @Override
  @Async
  public void sendVerificationEmail(
    String recipient,
    String fullName,
    String verificationUrl
  ) {
    log.info(
      "📧 [START] sendVerificationEmail - recipient: {}, fullName: {}",
      recipient,
      fullName
    );
    long startTime = System.currentTimeMillis();

    try {
      log.info("📝 [STEP 1] Creating email context for verification message");
      Context context = createContext(
        fullName,
        verificationUrl,
        verificationExpirationHours
      );
      context.setVariable("badgeText", "Secure your account");
      context.setVariable(
        "heading",
        "Verify your email and unlock your portfolio."
      );
      context.setVariable(
        "description",
        "Your Portfolio Hub workspace is ready. Confirm your email to upload files, complete your profile and start sharing your work."
      );
      context.setVariable("buttonLabel", "Verify my email");

      log.info(
        "📝 [STEP 2] Processing Thymeleaf template: portfolio-hub-verification-template"
      );
      String htmlBody = templateEngine.process(
        "portfolio-hub-verification-template",
        context
      );
      log.info(
        "✅ [STEP 3] Template processed successfully, HTML size: {} characters",
        htmlBody.length()
      );

      log.info("📤 [STEP 4] Sending email via SMTP to: {}", recipient);
      sendHtmlEmail(
        recipient,
        fullName,
        "Verify your Portfolio Hub email",
        htmlBody
      );

      long duration = System.currentTimeMillis() - startTime;
      log.info(
        "✅ [SUCCESS] Verification email sent to {} in {}ms",
        recipient,
        duration
      );
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error(
        "❌ [FAILED] Verification email failed after {}ms - recipient: {}, error: {}",
        duration,
        recipient,
        e.getMessage(),
        e
      );
    }
  }

  @Override
  @Async
  public void sendPasswordResetEmail(
    String recipient,
    String fullName,
    String resetUrl
  ) {
    log.info(
      "🔑 [START] sendPasswordResetEmail - recipient: {}, fullName: {}",
      recipient,
      fullName
    );
    long startTime = System.currentTimeMillis();

    try {
      log.info("📝 [STEP 1] Creating password reset context");
      Context context = createContext(
        fullName,
        resetUrl,
        passwordResetExpirationHours
      );
      context.setVariable("badgeText", "Password assistance");
      context.setVariable("heading", "Choose a new password.");
      context.setVariable(
        "description",
        "Use this secure, single-use link to set a new password for your Portfolio Hub account."
      );
      context.setVariable("buttonLabel", "Reset my password");

      log.info(
        "📝 [STEP 2] Processing template: portfolio-hub-password-reset-template"
      );
      String htmlBody = templateEngine.process(
        "portfolio-hub-password-reset-template",
        context
      );
      log.info(
        "✅ [STEP 3] Template processed, HTML size: {} characters",
        htmlBody.length()
      );

      log.info("📤 [STEP 4] Sending password reset email to: {}", recipient);
      sendHtmlEmail(
        recipient,
        fullName,
        "Reset your Portfolio Hub password",
        htmlBody
      );

      long duration = System.currentTimeMillis() - startTime;
      log.info(
        "✅ [SUCCESS] Password reset email sent to {} in {}ms",
        recipient,
        duration
      );
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error(
        "❌ [FAILED] Password reset email failed after {}ms - recipient: {}, error: {}",
        duration,
        recipient,
        e.getMessage(),
        e
      );
    }
  }

  @Override
  @Async
  public void sendNewEnquiryNotification(
    String recipient,
    String fullName,
    String recruiterName,
    String recruiterEmail,
    String company,
    String message,
    String dashboardUrl
  ) {
    log.info(
      "📬 [START] sendNewEnquiryNotification - recipient: {}, recruiter: {}, company: {}",
      recipient,
      recruiterName,
      company
    );
    long startTime = System.currentTimeMillis();

    try {
      log.info(
        "📝 [STEP 1] Creating enquiry context - recruiterEmail: {}, dashboardUrl: {}",
        recruiterEmail,
        dashboardUrl
      );
      Context context = new Context(Locale.getDefault());

      String safeName = fullName == null ? "there" : fullName;
      String safeCompany = company == null ? "Not provided" : company;

      log.info(
        "📝 [STEP 2] Setting context variables - fullName: {}, company: {}",
        safeName,
        safeCompany
      );
      context.setVariable("fullName", safeName);
      context.setVariable("recruiterName", recruiterName);
      context.setVariable("recruiterEmail", recruiterEmail);
      context.setVariable("company", safeCompany);
      context.setVariable("message", message);
      context.setVariable("dashboardUrl", dashboardUrl);
      context.setVariable("organizationName", organizationName);
      context.setVariable("supportEmail", supportEmail);
      context.setVariable("year", Year.now().getValue());

      log.info(
        "📝 [STEP 3] Processing template: portfolio-hub-enquiry-notification-template"
      );
      String htmlBody = templateEngine.process(
        "portfolio-hub-enquiry-notification-template",
        context
      );
      log.info(
        "✅ [STEP 4] Template processed, HTML size: {} characters",
        htmlBody.length()
      );

      log.info("📤 [STEP 5] Sending enquiry notification to: {}", recipient);
      sendHtmlEmail(
        recipient,
        fullName,
        "New recruiter enquiry from your portfolio",
        htmlBody
      );

      long duration = System.currentTimeMillis() - startTime;
      log.info(
        "✅ [SUCCESS] Enquiry notification sent to {} in {}ms",
        recipient,
        duration
      );
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error(
        "❌ [FAILED] Enquiry notification failed after {}ms - recipient: {}, error: {}",
        duration,
        recipient,
        e.getMessage(),
        e
      );
    }
  }

  @Override
  public void sendEmail(
    String senderEmail,
    String recipientEmail,
    String subject,
    String body
  ) {
    try {
      Context context = new Context(Locale.getDefault());
      context.setVariable("subject", subject);
      context.setVariable("message", body);
      context.setVariable("dashboardUrl", frontEndUrl());
      context.setVariable("organizationName", organizationName);
      context.setVariable("supportEmail", supportEmail);
      context.setVariable("year", Year.now().getValue());
      String htmlBody = templateEngine.process(
        "portfolio-hub-message-template",
        context
      );
      sendHtmlEmail(recipientEmail, null, subject, htmlBody);
    } catch (Exception exception) {
      log.error(
        "Unable to send account email to {}: {}",
        recipientEmail,
        exception.getMessage(),
        exception
      );
    }
  }

  private void sendHtmlEmail(
    String recipient,
    String fullName,
    String subject,
    String htmlBody
  ) {
    log.debug(
      "📤 [SMTP] Sending HTML email - from: {}, to: {}, subject: {}",
      fromEmail,
      recipient,
      subject
    );
    long startTime = System.currentTimeMillis();

    try {
      log.debug("📝 [SMTP] Creating MIME message with HTML content");
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      log.debug(
        "📝 [SMTP] Setting email headers - from: {}, to: {}",
        fromEmail,
        recipient
      );
      helper.setFrom(fromEmail, organizationName);
      helper.setTo(recipient);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);

      log.debug("📤 [SMTP] Sending via JavaMailSender");
      mailSender.send(message);

      long duration = System.currentTimeMillis() - startTime;
      log.info(
        "✅ [SMTP] Email sent successfully in {}ms - to: {}",
        duration,
        recipient
      );
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error(
        "❌ [SMTP] Failed after {}ms - to: {}, error: {}",
        duration,
        recipient,
        e.getMessage(),
        e
      );
      throw new RuntimeException("Failed to send HTML email", e);
    }
  }

  private Context createContext(
    String fullName,
    String url,
    long expirationHours
  ) {
    log.debug(
      "📝 [CONTEXT] Creating email context - fullName: {}, url: {}, expires: {}h",
      fullName,
      url,
      expirationHours
    );

    Context context = new Context(Locale.getDefault());
    String safeName = fullName == null ? "there" : fullName;

    context.setVariable("fullName", safeName);
    context.setVariable("url", url);
    context.setVariable("expirationHours", expirationHours);
    context.setVariable("organizationName", organizationName);
    context.setVariable("supportEmail", supportEmail);
    context.setVariable("year", Year.now().getValue());

    log.debug(
      "✅ [CONTEXT] Context created with variables: fullName={}, expirationHours={}",
      safeName,
      expirationHours
    );
    return context;
  }

  @Value("${application.front-end-url:http://localhost:3000}")
  private String frontEndUrl;

  private String frontEndUrl() {
    return frontEndUrl.replaceAll("/$", "") + "/dashboard";
  }
}
