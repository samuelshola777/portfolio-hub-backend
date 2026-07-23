package com.portfolio_hub.userauthmgt.user;

import com.portfolio_hub.utils.emailsenderservice.EmailSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class VerificationEmailListener {

  private final EmailSenderService emailSenderService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void send(VerificationEmailRequested event) {
    emailSenderService.sendVerificationEmail(
      event.email(),
      event.fullName(),
      event.verificationUrl()
    );
  }
}
