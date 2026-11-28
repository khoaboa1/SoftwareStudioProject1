package com.Handoff.backend.service;

import com.Handoff.backend.model.Role;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.service.auth.EmailNotConfirmedException;
import com.Handoff.backend.service.auth.LoginOutcome;
import com.Handoff.backend.service.auth.PendingLoginStore;
import com.Handoff.backend.service.auth.SupabaseAdminUser;
import com.Handoff.backend.service.auth.SupabaseAuthClient;
import com.Handoff.backend.service.auth.SupabaseAuthException;
import com.Handoff.backend.service.auth.SupabaseSession;
import com.Handoff.backend.service.auth.VerifiedLogin;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final String EMAIL = "jane@tulane.edu";
    private static final String PASSWORD = "Password123!";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final SupabaseSession SESSION = new SupabaseSession("access", "refresh", 3600, USER_ID);
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private VerificationService verificationService;
    @Mock
    private SupabaseAuthClient supabase;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private PendingLoginStore pendingLogins;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        pendingLogins = new PendingLoginStore();
        authService = new AuthService(studentRepository, verificationService, new UniversityEmailValidator(), supabase,
                pendingLogins, passwordEncoder);
    }

    private Student verifiedStudent(String... trustedDevices) {
        Student student = new Student("Jane Doe", EMAIL, null, null, null);
        student.setId(7L);
        student.setVerified(true);
        student.setEmailVerified(true);
        for (String device : trustedDevices) {
            student.trustDevice(device);
        }
        return student;
    }

    private Student unverifiedStudent() {
        Student student = new Student("Jane Doe", EMAIL, null, null, null);
        student.setId(7L);
        return student;
    }

    private Student legacyStudent(String password) {
        Student student = verifiedStudent("device-1");
        student.setPasswordHash(passwordEncoder.encode(password));
        return student;
    }

    private void saveReturnsArgument() {
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void signup_createsUnconfirmedSupabaseUser_andSendsOurPin() {
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(supabase.adminCreateUnconfirmedUser(EMAIL, PASSWORD)).thenReturn(USER_ID);
        saveReturnsArgument();
        Student student = authService.signup("Jane Doe", EMAIL, PASSWORD);
        assertThat(student.getEmail()).isEqualTo(EMAIL);
        assertThat(student.getStudentName()).isEqualTo("Jane Doe");
        assertThat(student.getSupabaseUserId()).isEqualTo(USER_ID);
        assertThat(student.getPasswordHash()).isNull();
        assertThat(student.isVerified()).isFalse();
        verify(verificationService).sendPinUnlessLimited(student);
    }

    @Test
    void signup_newStudentHasStudentRole() {
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(supabase.adminCreateUnconfirmedUser(EMAIL, PASSWORD)).thenReturn(USER_ID);
        saveReturnsArgument();
        Student student = authService.signup("Jane Doe", EMAIL, PASSWORD);
        assertThat(student.getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void signup_rejectsVerifiedEmailBeforeCallingSupabase() {
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedStudent()));
        assertThatThrownBy(() -> authService.signup("Jane Doe", EMAIL, PASSWORD))
                .isExactlyInstanceOf(EmailAlreadyRegisteredException.class).hasMessage("Email already registered");
        verifyNoInteractions(supabase, verificationService);
    }

    @Test
    void signup_reusesUnverifiedStudentAndItsSupabaseUser() {
        Student existing = unverifiedStudent();
        existing.setSupabaseUserId(USER_ID);
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));
        saveReturnsArgument();
        Student student = authService.signup("Jane Q. Doe", EMAIL, PASSWORD);
        assertThat(student).isSameAs(existing);
        assertThat(student.getStudentName()).isEqualTo("Jane Q. Doe");
        assertThat(student.getSupabaseUserId()).isEqualTo(USER_ID);
        verify(supabase).adminUpdateUser(USER_ID, PASSWORD, false);
        verify(supabase, never()).adminCreateUnconfirmedUser(anyString(), anyString());
        verify(verificationService).sendPinUnlessLimited(student);
    }

    @Test
    void signup_adoptsUnconfirmedSupabaseUserThatOutlivedOurRow() {
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(supabase.adminCreateUnconfirmedUser(EMAIL, PASSWORD))
                .thenThrow(new EmailAlreadyRegisteredException("Email already registered"));
        when(supabase.adminFindUserByEmail(EMAIL)).thenReturn(Optional.of(new SupabaseAdminUser(USER_ID, false)));
        saveReturnsArgument();
        Student student = authService.signup("Jane Doe", EMAIL, PASSWORD);
        assertThat(student.getSupabaseUserId()).isEqualTo(USER_ID);
        verify(supabase).adminUpdateUser(USER_ID, PASSWORD, false);
        verify(verificationService).sendPinUnlessLimited(student);
    }

    @Test
    void signup_rejectsEmailOfAConfirmedSupabaseUser() {
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(supabase.adminCreateUnconfirmedUser(EMAIL, PASSWORD))
                .thenThrow(new EmailAlreadyRegisteredException("Email already registered"));
        when(supabase.adminFindUserByEmail(EMAIL)).thenReturn(Optional.of(new SupabaseAdminUser(USER_ID, true)));
        assertThatThrownBy(() -> authService.signup("Jane Doe", EMAIL, PASSWORD))
                .isExactlyInstanceOf(EmailAlreadyRegisteredException.class);
        verify(supabase, never()).adminUpdateUser(any(), any(), any());
        verifyNoInteractions(verificationService);
    }

    @Test
    void login_onTrustedDevice_returnsSuccessAndSession() {
        Student student = verifiedStudent("device-1");
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(student));
        when(supabase.passwordGrant(EMAIL, PASSWORD)).thenReturn(SESSION);
        saveReturnsArgument();
        LoginOutcome outcome = authService.login(EMAIL, PASSWORD, "device-1");
        assertThat(outcome.session()).isEqualTo(SESSION);
        assertThat(outcome.response().isRequiresPin()).isFalse();
        assertThat(outcome.response().getEmail()).isEqualTo(EMAIL);
        assertThat(student.getSupabaseUserId()).isEqualTo(USER_ID);
        verifyNoInteractions(verificationService);
    }

    @Test
    void login_onNewDevice_holdsSessionAndSendsDevicePin() {
        Student student = verifiedStudent("device-1");
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(student));
        when(supabase.passwordGrant(EMAIL, PASSWORD)).thenReturn(SESSION);
        saveReturnsArgument();
        LoginOutcome outcome = authService.login(EMAIL, PASSWORD, "device-2");
        assertThat(outcome.session()).isNull();
        assertThat(outcome.response().isRequiresPin()).isTrue();
        assertThat(outcome.response().getMessage()).isEqualTo(AuthService.NEW_DEVICE_MESSAGE);
        assertThat(pendingLogins.get(EMAIL, "device-2")).contains(SESSION);
        verify(verificationService).sendPinUnlessLimited(student);
    }

    @Test
    void login_withoutDeviceId_requiresDevicePin() {
        Student student = verifiedStudent("device-1");
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(student));
        when(supabase.passwordGrant(EMAIL, PASSWORD)).thenReturn(SESSION);
        saveReturnsArgument();
        LoginOutcome outcome = authService.login(EMAIL, PASSWORD, null);
        assertThat(outcome.session()).isNull();
        assertThat(outcome.response().isRequiresPin()).isTrue();
        verify(verificationService).sendPinUnlessLimited(student);
    }

    @Test
    void login_withUnconfirmedEmail_ofUnverifiedStudent_sendsOurPin() {
        Student student = unverifiedStudent();
        student.setSupabaseUserId(USER_ID);
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(student));
        when(supabase.passwordGrant(EMAIL, PASSWORD)).thenThrow(new EmailNotConfirmedException());
        LoginOutcome outcome = authService.login(EMAIL, PASSWORD, "device-1");
        assertThat(outcome.session()).isNull();
        assertThat(outcome.response().isRequiresPin()).isTrue();
        assertThat(outcome.response().getMessage()).isEqualTo(AuthService.UNVERIFIED_MESSAGE);
        verify(verificationService).sendPinUnlessLimited(student);
        verify(supabase, never()).adminUpdateUser(any(), any(), any());
    }

    @Test
    void login_confirmsSupabaseEmail_whenOurPinWasAlreadyVerified() {
        Student student = verifiedStudent("device-1");
        student.setSupabaseUserId(USER_ID);
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(student));
        when(supabase.passwordGrant(EMAIL, PASSWORD)).thenThrow(new EmailNotConfirmedException()).thenReturn(SESSION);
        saveReturnsArgument();
        LoginOutcome outcome = authService.login(EMAIL, PASSWORD, "device-1");
        assertThat(outcome.session()).isEqualTo(SESSION);
        assertThat(outcome.response().isRequiresPin()).isFalse();
        verify(supabase).adminUpdateUser(USER_ID, null, true);
    }

    @Test
    void login_ofUnverifiedStudentWhoseSupabaseUserIsConfirmed_stillRequiresOurPin() {
        Student student = unverifiedStudent();
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(student));
        when(supabase.passwordGrant(EMAIL, PASSWORD)).thenReturn(SESSION);
        saveReturnsArgument();
        LoginOutcome outcome = authService.login(EMAIL, PASSWORD, "device-1");
        assertThat(outcome.session()).isNull();
        assertThat(outcome.response().isRequiresPin()).isTrue();
        assertThat(outcome.response().getMessage()).isEqualTo(AuthService.UNVERIFIED_MESSAGE);
        assertThat(student.isVerified()).isFalse();
        assertThat(student.getSupabaseUserId()).isEqualTo(USER_ID);
        verify(verificationService).sendPinUnlessLimited(student);
    }

    @Test
    void login_withUnknownEmail_throwsWithoutCallingSupabase() {
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD, "device-1"))
                .isExactlyInstanceOf(InvalidCredentialsException.class).hasMessage("Invalid email or password");
        verifyNoInteractions(supabase);
    }

    @Test
    void login_withWrongPassword_propagatesInvalidCredentials() {
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedStudent("device-1")));
        when(supabase.passwordGrant(EMAIL, "wrong-password")).thenThrow(new InvalidCredentialsException());
        assertThatThrownBy(() -> authService.login(EMAIL, "wrong-password", "device-1"))
                .isExactlyInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_migratesLegacyAccount_whenOldPasswordMatches() {
        Student student = legacyStudent(PASSWORD);
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(student));
        when(supabase.passwordGrant(EMAIL, PASSWORD)).thenThrow(new InvalidCredentialsException()).thenReturn(SESSION);
        when(supabase.adminCreateConfirmedUser(EMAIL, PASSWORD)).thenReturn(USER_ID);
        saveReturnsArgument();
        LoginOutcome outcome = authService.login(EMAIL, PASSWORD, "device-1");
        assertThat(outcome.session()).isEqualTo(SESSION);
        assertThat(outcome.response().isRequiresPin()).isFalse();
        assertThat(student.getSupabaseUserId()).isEqualTo(USER_ID);
        assertThat(student.getPasswordHash()).isNull();
    }

    @Test
    void login_doesNotMigrateLegacyAccount_whenOldPasswordDiffers() {
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(legacyStudent(PASSWORD)));
        when(supabase.passwordGrant(EMAIL, "wrong-password")).thenThrow(new InvalidCredentialsException());
        assertThatThrownBy(() -> authService.login(EMAIL, "wrong-password", "device-1"))
                .isExactlyInstanceOf(InvalidCredentialsException.class);
        verify(supabase, never()).adminCreateConfirmedUser(anyString(), anyString());
    }

    @Test
    void login_doesNotMigrateUnverifiedLegacyAccount() {
        Student student = unverifiedStudent();
        student.setPasswordHash(passwordEncoder.encode(PASSWORD));
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(student));
        when(supabase.passwordGrant(EMAIL, PASSWORD)).thenThrow(new InvalidCredentialsException());
        assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD, "device-1"))
                .isExactlyInstanceOf(InvalidCredentialsException.class);
        verify(supabase, never()).adminCreateConfirmedUser(anyString(), anyString());
    }

    @Test
    void login_doesNotMigrateAccountAlreadyLinkedToSupabase() {
        Student student = legacyStudent(PASSWORD);
        student.setSupabaseUserId(USER_ID);
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(student));
        when(supabase.passwordGrant(EMAIL, PASSWORD)).thenThrow(new InvalidCredentialsException());
        assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD, "device-1"))
                .isExactlyInstanceOf(InvalidCredentialsException.class);
        verify(supabase, never()).adminCreateConfirmedUser(anyString(), anyString());
    }

    @Test
    void login_keepsInvalidCredentials_whenSupabaseRejectsMigration() {
        Student student = legacyStudent(PASSWORD);
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(student));
        when(supabase.passwordGrant(EMAIL, PASSWORD)).thenThrow(new InvalidCredentialsException());
        when(supabase.adminCreateConfirmedUser(EMAIL, PASSWORD))
                .thenThrow(new SupabaseAuthException(503, "not_configured", "No service key"));
        assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD, "device-1"))
                .isExactlyInstanceOf(InvalidCredentialsException.class).hasMessage("Invalid email or password");
        assertThat(student.getPasswordHash()).isNotNull();
    }

    @Test
    void verifyPin_forUnverifiedStudent_checksOurPin_confirmsSupabase_andIssuesNoSession() {
        Student student = unverifiedStudent();
        Student verified = verifiedStudent("device-1");
        verified.setSupabaseUserId(USER_ID);
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(student));
        when(verificationService.verifyPin(EMAIL, "123456", "device-1")).thenReturn(verified);
        VerifiedLogin result = authService.verifyPin(EMAIL, "123456", "device-1");
        assertThat(result.session()).isNull();
        assertThat(result.student()).isSameAs(verified);
        verify(supabase).adminUpdateUser(USER_ID, null, true);
    }

    @Test
    void verifyPin_forUnverifiedStudent_ignoresSupabaseConfirmFailure() {
        Student verified = verifiedStudent("device-1");
        verified.setSupabaseUserId(USER_ID);
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedStudent()));
        when(verificationService.verifyPin(EMAIL, "123456", "device-1")).thenReturn(verified);
        doThrow(new SupabaseAuthException(502, "unreachable", "down")).when(supabase).adminUpdateUser(USER_ID, null,
                true);
        assertThatCode(() -> authService.verifyPin(EMAIL, "123456", "device-1")).doesNotThrowAnyException();
    }

    @Test
    void verifyPin_forUnverifiedStudentWithoutSupabaseUser_skipsTheConfirmCall() {
        Student verified = verifiedStudent("device-1");
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedStudent()));
        when(verificationService.verifyPin(EMAIL, "123456", "device-1")).thenReturn(verified);
        VerifiedLogin result = authService.verifyPin(EMAIL, "123456", "device-1");
        assertThat(result.session()).isNull();
        verifyNoInteractions(supabase);
    }

    @Test
    void verifyPin_forUnverifiedStudent_withWrongPin_doesNotConfirmSupabase() {
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedStudent()));
        when(verificationService.verifyPin(EMAIL, "000000", "device-1"))
                .thenThrow(new InvalidVerificationPinException("Incorrect verification PIN"));
        assertThatThrownBy(() -> authService.verifyPin(EMAIL, "000000", "device-1"))
                .isExactlyInstanceOf(InvalidVerificationPinException.class).hasMessage("Incorrect verification PIN");
        verifyNoInteractions(supabase);
    }

    @Test
    void verifyPin_forNewDevice_checksDevicePinAndReleasesHeldSession() {
        Student student = verifiedStudent("device-1");
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(student));
        pendingLogins.put(EMAIL, "device-2", SESSION);
        when(verificationService.verifyPin(EMAIL, "654321", "device-2")).thenReturn(student);
        VerifiedLogin result = authService.verifyPin(EMAIL, "654321", "device-2");
        assertThat(result.session()).isEqualTo(SESSION);
        assertThat(result.student()).isSameAs(student);
        assertThat(pendingLogins.get(EMAIL, "device-2")).isEmpty();
        verifyNoInteractions(supabase);
    }

    @Test
    void verifyPin_forNewDeviceWithoutHeldLogin_asksToLogInAgain() {
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedStudent("device-1")));
        assertThatThrownBy(() -> authService.verifyPin(EMAIL, "654321", "device-2"))
                .isExactlyInstanceOf(InvalidCredentialsException.class).hasMessage(AuthService.LOGIN_EXPIRED_MESSAGE);
        verifyNoInteractions(verificationService);
    }

    @Test
    void verifyPin_withWrongDevicePin_keepsHeldLogin() {
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedStudent("device-1")));
        pendingLogins.put(EMAIL, "device-2", SESSION);
        when(verificationService.verifyPin(EMAIL, "000000", "device-2"))
                .thenThrow(new InvalidVerificationPinException("Incorrect verification PIN"));
        assertThatThrownBy(() -> authService.verifyPin(EMAIL, "000000", "device-2"))
                .isExactlyInstanceOf(InvalidVerificationPinException.class).hasMessage("Incorrect verification PIN");
        assertThat(pendingLogins.get(EMAIL, "device-2")).contains(SESSION);
    }

    @Test
    void verifyPin_requiresPin() {
        assertThatThrownBy(() -> authService.verifyPin(EMAIL, " ", "device-1"))
                .isExactlyInstanceOf(InvalidVerificationPinException.class).hasMessage("Verification PIN is required");
        verifyNoInteractions(studentRepository, supabase, verificationService);
    }

    @Test
    void resendPin_resendsOurPin() {
        authService.resendPin(EMAIL);
        verify(verificationService).resendPin(EMAIL);
        verifyNoInteractions(supabase);
    }

    @Test
    void resendPin_rejectsNonSchoolEmail() {
        assertThatThrownBy(() -> authService.resendPin("jane@gmail.com"))
                .isExactlyInstanceOf(InvalidSignupException.class).hasMessage("Use a valid .edu school email");
        verifyNoInteractions(verificationService, supabase);
    }

    @Test
    void refresh_withoutToken_asksToLogInAgain() {
        assertThatThrownBy(() -> authService.refresh(null)).isExactlyInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Please log in again.");
        verifyNoInteractions(supabase);
    }

    @Test
    void refresh_returnsNewSupabaseSession() {
        when(supabase.refreshGrant("refresh")).thenReturn(SESSION);
        assertThat(authService.refresh("refresh")).isEqualTo(SESSION);
    }

    @Test
    void logout_ignoresSupabaseFailures() {
        doThrow(new SupabaseAuthException(502, "unreachable", "down")).when(supabase).logout("access");
        assertThatCode(() -> authService.logout("access")).doesNotThrowAnyException();
    }

    @Test
    void logout_withoutToken_skipsSupabase() {
        authService.logout(null);
        verifyNoInteractions(supabase);
    }

    @Test
    void findBySupabaseUserId_delegatesToRepository() {
        Student student = verifiedStudent();
        when(studentRepository.findBySupabaseUserId(USER_ID)).thenReturn(Optional.of(student));
        assertThat(authService.findBySupabaseUserId(USER_ID)).contains(student);
    }
}
