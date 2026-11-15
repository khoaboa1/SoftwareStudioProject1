package com.Handoff.backend.config;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.service.InvalidCredentialsException;
import com.Handoff.backend.service.auth.SupabaseAuthClient;
import com.Handoff.backend.service.auth.SupabaseAuthException;
import com.Handoff.backend.service.auth.SupabaseSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupabaseSeedSyncTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SupabaseAuthClient supabase;

    private SupabaseSeedSync sync() {
        return new SupabaseSeedSync(studentRepository, supabase);
    }

    private Student seedStudentNamedSarah() {
        Student sarah = new Student("Sarah", "sarah@tulane.edu", null, null, null);
        when(supabase.isConfigured()).thenReturn(true);
        when(studentRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(studentRepository.findByEmail("sarah@tulane.edu")).thenReturn(Optional.of(sarah));
        return sarah;
    }

    @Test
    void syncSeedUsers_doesNothing_whenSupabaseIsNotConfigured() {
        when(supabase.isConfigured()).thenReturn(false);
        assertThat(sync().syncSeedUsers()).isZero();
        verifyNoInteractions(studentRepository);
    }

    @Test
    void syncSeedUsers_createsConfirmedSupabaseUserForSeedStudent() {
        Student sarah = seedStudentNamedSarah();
        when(supabase.adminCreateConfirmedUser("sarah@tulane.edu", DataSeeder.SEED_PASSWORD)).thenReturn(USER_ID);
        assertThat(sync().syncSeedUsers()).isEqualTo(1);
        assertThat(sarah.getSupabaseUserId()).isEqualTo(USER_ID);
        verify(studentRepository).save(sarah);
    }

    @Test
    void syncSeedUsers_linksExistingSupabaseUser_whenCreateFails() {
        Student sarah = seedStudentNamedSarah();
        when(supabase.adminCreateConfirmedUser("sarah@tulane.edu", DataSeeder.SEED_PASSWORD))
                .thenThrow(new SupabaseAuthException(422, "email_exists", "Email exists"));
        when(supabase.passwordGrant("sarah@tulane.edu", DataSeeder.SEED_PASSWORD))
                .thenReturn(new SupabaseSession("access", "refresh", 3600, USER_ID));
        assertThat(sync().syncSeedUsers()).isEqualTo(1);
        assertThat(sarah.getSupabaseUserId()).isEqualTo(USER_ID);
    }

    @Test
    void syncSeedUsers_skipsStudentWhenSupabaseRejectsBothCalls() {
        Student sarah = seedStudentNamedSarah();
        when(supabase.adminCreateConfirmedUser("sarah@tulane.edu", DataSeeder.SEED_PASSWORD))
                .thenThrow(new SupabaseAuthException(503, "not_configured", "No service key"));
        when(supabase.passwordGrant("sarah@tulane.edu", DataSeeder.SEED_PASSWORD))
                .thenThrow(new InvalidCredentialsException());
        assertThat(sync().syncSeedUsers()).isZero();
        assertThat(sarah.getSupabaseUserId()).isNull();
        verify(studentRepository, never()).save(any());
    }

    @Test
    void syncSeedUsers_skipsStudentsAlreadyLinked() {
        Student sarah = seedStudentNamedSarah();
        sarah.setSupabaseUserId(USER_ID);
        assertThat(sync().syncSeedUsers()).isZero();
        verify(supabase, never()).adminCreateConfirmedUser(anyString(), anyString());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void seedEmails_coverBaseAndDemoSeedAccountsOnly() {
        assertThat(SupabaseSeedSync.seedEmails()).containsAll(DataSeeder.SEED_EMAILS)
                .contains(DemoSeedData.STUDENTS.get(0).email()).doesNotHaveDuplicates();
    }
}
