package com.Handoff.backend.service.email;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {

  @Mock
  private JavaMailSender javaMailSender;

  @Test
  void webhookSender_skipsWhenUrlNotConfigured() {
    assertThat(new WebhookEmailSender("").send("bob@tulane.edu", "123456")).isFalse();
    assertThat(new WebhookEmailSender(null).send("bob@tulane.edu", "123456")).isFalse();
  }

  @Test
  void resendSender_skipsWhenApiKeyNotConfigured() {
    assertThat(new ResendEmailSender("").send("bob@tulane.edu", "123456")).isFalse();
    assertThat(new ResendEmailSender(null).send("bob@tulane.edu", "123456")).isFalse();
  }

  @Test
  void smtpSender_skipsWhenNoMailSenderBean() {
    assertThat(new SmtpEmailSender(null, "").send("bob@tulane.edu", "123456")).isFalse();
  }

  @Test
  void smtpSender_sendsPinMessage() {
    SmtpEmailSender sender = new SmtpEmailSender(javaMailSender, "handoff@gmail.com");

    assertThat(sender.send("bob@tulane.edu", "123456")).isTrue();

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(javaMailSender).send(captor.capture());
    SimpleMailMessage message = captor.getValue();
    assertThat(message.getTo()).containsExactly("bob@tulane.edu");
    assertThat(message.getFrom()).isEqualTo("handoff@gmail.com");
    assertThat(message.getSubject()).contains("123456");
    assertThat(message.getText()).contains("123456");
  }

  @Test
  void smtpSender_returnsFalseWhenSmtpFails() {
    doThrow(new MailSendException("down")).when(javaMailSender).send(any(SimpleMailMessage.class));

    assertThat(new SmtpEmailSender(javaMailSender, "").send("bob@tulane.edu", "123456")).isFalse();
  }

  @Test
  void senders_areOrderedWebhookThenResendThenSmtp() {
    List<EmailSender> senders = new ArrayList<>(List.of(
        new SmtpEmailSender(null, ""),
        new ResendEmailSender(""),
        new WebhookEmailSender("")));

    AnnotationAwareOrderComparator.sort(senders);

    assertThat(senders).extracting(s -> s.getClass().getSimpleName())
        .containsExactly("WebhookEmailSender", "ResendEmailSender", "SmtpEmailSender");
  }
}
