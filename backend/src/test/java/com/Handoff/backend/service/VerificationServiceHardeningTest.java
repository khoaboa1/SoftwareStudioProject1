package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VerificationServiceHardeningTest {

    private final StudentRepository studentRepository = mock(StudentRepository.class);
    private final UniversityEmailValidator validator = new UniversityEmailValidator();

    private VerificationService service() {
        return new VerificationService(studentRepository, List.of(), validator);
    }

    private VerificationService serviceWithLogging() {
        return new VerificationService(studentRepository, List.of(), validator, true);
    }

    private Student student() {
        Student student = new Student("Test", "a@tulane.edu", null, null, null);
        student.setVerificationPin("123456");
        student.setVerificationPinExpiry(LocalDateTime.now().plusMinutes(10));
        return student;
    }

    private void givenStudent(Student student) {
        when(studentRepository.findByEmail("a@tulane.edu")).thenReturn(Optional.of(student));
        when(studentRepository.save(student)).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void fourWrongPins_stillAllowTheCorrectPin() {
        VerificationService service = service();
        Student student = student();
        givenStudent(student);

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.verifyPin("a@tulane.edu", "000000", "dev"))
                    .isExactlyInstanceOf(InvalidVerificationPinException.class)
                    .hasMessage("Incorrect verification PIN");
        }

        Student verified = service.verifyPin("a@tulane.edu", "123456", "dev");
        assertThat(verified.isVerified()).isTrue();
        assertThat(verified.isEmailVerified()).isTrue();
        assertThat(verified.getVerificationPin()).isNull();
        assertThat(verified.getVerificationPinExpiry()).isNull();
    }

    @Test
    void fifthWrongPin_locksThePin() {
        VerificationService service = service();
        Student student = student();
        givenStudent(student);

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.verifyPin("a@tulane.edu", "000000", "dev"))
                    .isExactlyInstanceOf(InvalidVerificationPinException.class)
                    .hasMessage("Incorrect verification PIN");
        }

        assertThatThrownBy(() -> service.verifyPin("a@tulane.edu", "000000", "dev"))
                .isExactlyInstanceOf(InvalidVerificationPinException.class)
                .hasMessage("Too many incorrect attempts. Please request a new PIN.");

        assertThat(student.getVerificationPin()).isNull();
        assertThat(student.getVerificationPinExpiry()).isNull();

        assertThatThrownBy(() -> service.verifyPin("a@tulane.edu", "123456", "dev"))
                .isExactlyInstanceOf(InvalidVerificationPinException.class)
                .hasMessage("No active verification PIN found. Please request a new one.");
    }

    @Test
    void newPin_resetsTheCounter() {
        VerificationService service = service();
        Student student = student();
        givenStudent(student);

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.verifyPin("a@tulane.edu", "000000", "dev"))
                    .isExactlyInstanceOf(InvalidVerificationPinException.class)
                    .hasMessage("Incorrect verification PIN");
        }

        service.generateAndSendPin(student);

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.verifyPin("a@tulane.edu", "000000", "dev"))
                    .isExactlyInstanceOf(InvalidVerificationPinException.class)
                    .hasMessage("Incorrect verification PIN");
        }
    }

    @Test
    void successfulVerify_resetsTheCounter() {
        VerificationService service = service();
        Student student = student();
        givenStudent(student);

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.verifyPin("a@tulane.edu", "000000", "dev"))
                    .isExactlyInstanceOf(InvalidVerificationPinException.class)
                    .hasMessage("Incorrect verification PIN");
        }

        Student verified = service.verifyPin("a@tulane.edu", "123456", "dev");
        assertThat(verified.isVerified()).isTrue();

        student.setVerificationPin("123456");
        student.setVerificationPinExpiry(LocalDateTime.now().plusMinutes(10));

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.verifyPin("a@tulane.edu", "000000", "dev"))
                    .isExactlyInstanceOf(InvalidVerificationPinException.class)
                    .hasMessage("Incorrect verification PIN");
        }
    }

    @Test
    void expiredPin_doesNotCountAsAGuess() {
        VerificationService service = service();
        Student student = student();
        student.setVerificationPinExpiry(LocalDateTime.now().minusMinutes(1));
        givenStudent(student);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.verifyPin("a@tulane.edu", "000000", "dev"))
                    .isExactlyInstanceOf(InvalidVerificationPinException.class)
                    .hasMessage("Verification PIN has expired. Please request a new one.");
        }

        student.setVerificationPinExpiry(LocalDateTime.now().plusMinutes(10));

        assertThatThrownBy(() -> service.verifyPin("a@tulane.edu", "000000", "dev"))
                .isExactlyInstanceOf(InvalidVerificationPinException.class).hasMessage("Incorrect verification PIN");
    }

    @Test
    void pinLogMessage_hidesThePinByDefault() {
        String message = service().pinLogMessage("a@tulane.edu", "654321");
        assertThat(message).doesNotContain("654321");
        assertThat(message).contains("a@tulane.edu");
    }

    @Test
    void pinLogMessage_showsThePinWhenEnabled() {
        String message = serviceWithLogging().pinLogMessage("a@tulane.edu", "654321");
        assertThat(message).contains("PIN: 654321");
    }
}
