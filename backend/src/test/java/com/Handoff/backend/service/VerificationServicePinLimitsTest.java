package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.service.email.EmailSender;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationServicePinLimitsTest {
    /** A clock the test can move forward. */
    static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-11-28T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final StudentRepository studentRepository = mock(StudentRepository.class);
    private final EmailSender sender = mock(EmailSender.class);
    private final UniversityEmailValidator validator = new UniversityEmailValidator();
    private final MovableClock clock = new MovableClock();
    private Student student;

    private VerificationService service() {
        return new VerificationService(studentRepository, List.of(sender), validator, false, Duration.ofSeconds(60), 5,
                clock);
    }

    @BeforeEach
    void setUp() {
        student = new Student("Test", "a@tulane.edu", null, null, null);
        when(studentRepository.findByEmail("a@tulane.edu")).thenReturn(Optional.of(student));
        when(studentRepository.save(student)).thenAnswer(i -> i.getArgument(0));
        when(sender.send(anyString(), anyString())).thenReturn(true);
    }

    private void givenActivePin() {
        student.setVerificationPin("123456");
        student.setVerificationPinExpiry(LocalDateTime.now(clock).plusMinutes(10));
    }

    @Test
    void wrongGuessesAreStoredOnTheStudent() {
        givenActivePin();
        VerificationService service = service();
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> service.verifyPin("a@tulane.edu", "000000", "dev"))
                    .hasMessage("Incorrect verification PIN");
        }
        assertThat(student.getPinFailedAttempts()).isEqualTo(2);
        verify(studentRepository, times(2)).save(student);
    }

    @Test
    void theCountSurvivesARestart() {
        givenActivePin();
        VerificationService beforeRestart = service();
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> beforeRestart.verifyPin("a@tulane.edu", "000000", "dev"))
                    .hasMessage("Incorrect verification PIN");
        }
        VerificationService afterRestart = service();
        assertThatThrownBy(() -> afterRestart.verifyPin("a@tulane.edu", "000000", "dev"))
                .hasMessage("Too many incorrect attempts. Please request a new PIN.");
        assertThat(student.getVerificationPin()).isNull();
        assertThat(student.getVerificationPinExpiry()).isNull();
        assertThat(student.getPinFailedAttempts()).isZero();
    }

    @Test
    void aCorrectPinResetsTheCount() {
        givenActivePin();
        student.setPinFailedAttempts(3);
        service().verifyPin("a@tulane.edu", "123456", "dev");
        assertThat(student.getPinFailedAttempts()).isZero();
    }

    @Test
    void aNewPinResetsTheCountAndRecordsTheSend() {
        student.setPinFailedAttempts(3);
        service().generateAndSendPin(student);
        assertThat(student.getPinFailedAttempts()).isZero();
        assertThat(student.getPinLastSentAt()).isEqualTo(LocalDateTime.now(clock));
        assertThat(student.getPinSendWindowStart()).isEqualTo(LocalDateTime.now(clock));
        assertThat(student.getPinSendCount()).isEqualTo(1);
    }

    @Test
    void resendingInsideTheCooldownIsRefusedWithTheWaitTime() {
        VerificationService service = service();
        service.generateAndSendPin(student);
        String firstPin = student.getVerificationPin();
        assertThatThrownBy(() -> service.resendPin("a@tulane.edu")).isInstanceOf(PinRequestLimitException.class)
                .hasMessage("Please wait 60 seconds before requesting a new PIN.").extracting("retryAfterSeconds")
                .isEqualTo(60L);
        clock.advance(Duration.ofSeconds(30));
        assertThatThrownBy(() -> service.resendPin("a@tulane.edu"))
                .hasMessage("Please wait 30 seconds before requesting a new PIN.");
        assertThat(student.getVerificationPin()).isEqualTo(firstPin);
        verify(sender, times(1)).send(anyString(), anyString());
    }

    @Test
    void resendingAfterTheCooldownSendsANewPin() {
        VerificationService service = service();
        service.generateAndSendPin(student);
        clock.advance(Duration.ofSeconds(60));
        service.resendPin("a@tulane.edu");
        verify(sender, times(2)).send(anyString(), anyString());
        assertThat(student.getPinSendCount()).isEqualTo(2);
    }

    @Test
    void theSixthPinInAnHourIsRefusedUntilTheWindowEnds() {
        VerificationService service = service();
        for (int i = 0; i < 5; i++) {
            service.resendPin("a@tulane.edu");
            clock.advance(Duration.ofSeconds(61));
        }
        assertThatThrownBy(() -> service.resendPin("a@tulane.edu")).isInstanceOf(PinRequestLimitException.class)
                .hasMessage("Too many PIN requests. Please try again in 55 minutes.").extracting("retryAfterSeconds")
                .isEqualTo(3600L - 5 * 61L);
        clock.advance(Duration.ofSeconds(3600 - 5 * 61));
        service.resendPin("a@tulane.edu");
        verify(sender, times(6)).send(anyString(), anyString());
        assertThat(student.getPinSendCount()).isEqualTo(1);
        assertThat(student.getPinSendWindowStart()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    void loginInsideTheCooldownKeepsTheCurrentPin() {
        VerificationService service = service();
        service.generateAndSendPin(student);
        String firstPin = student.getVerificationPin();
        boolean sent = service.sendPinUnlessLimited(student);
        assertThat(sent).isFalse();
        assertThat(student.getVerificationPin()).isEqualTo(firstPin);
        verify(sender, times(1)).send(anyString(), anyString());
    }

    @Test
    void loginOutsideTheLimitsSendsAPin() {
        boolean sent = service().sendPinUnlessLimited(student);
        assertThat(sent).isTrue();
        assertThat(student.getVerificationPin()).matches("\\d{6}");
        verify(sender, times(1)).send(anyString(), anyString());
    }

    @Test
    void aRefusedResendDoesNotTouchTheStudent() {
        VerificationService service = service();
        service.generateAndSendPin(student);
        student.setPinFailedAttempts(2);
        assertThatThrownBy(() -> service.resendPin("a@tulane.edu")).isInstanceOf(PinRequestLimitException.class);
        assertThat(student.getPinFailedAttempts()).isEqualTo(2);
        assertThat(student.getPinSendCount()).isEqualTo(1);
        verify(sender, times(1)).send(anyString(), anyString());
    }
}
