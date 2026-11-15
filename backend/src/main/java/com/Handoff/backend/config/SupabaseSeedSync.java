package com.Handoff.backend.config;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.service.auth.SupabaseAuthClient;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Links the seeded demo accounts to Supabase Auth users so they can log in with the seed password. Only known seed
 * emails are touched; real accounts are never created or changed here.
 */
@Component
public class SupabaseSeedSync {
    private static final Logger log = LoggerFactory.getLogger(SupabaseSeedSync.class);
    private final StudentRepository studentRepository;
    private final SupabaseAuthClient supabase;

    public SupabaseSeedSync(StudentRepository studentRepository, SupabaseAuthClient supabase) {
        this.studentRepository = studentRepository;
        this.supabase = supabase;
    }

    /** Runs after the CommandLineRunner seeders have created the seed students. */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        syncSeedUsers();
    }

    public int syncSeedUsers() {
        if (!supabase.isConfigured()) {
            log.warn("Supabase Auth is not configured; seeded accounts cannot log in until "
                    + "SUPABASE_URL and SUPABASE_ANON_KEY are set.");
            return 0;
        }
        int linked = 0;
        for (String email : seedEmails()) {
            Optional<Student> found = studentRepository.findByEmail(email);
            if (found.isEmpty() || found.get().getSupabaseUserId() != null) {
                continue;
            }
            UUID supabaseUserId = createOrFind(email);
            if (supabaseUserId == null) {
                continue;
            }
            Student student = found.get();
            student.setSupabaseUserId(supabaseUserId);
            studentRepository.save(student);
            linked++;
        }
        if (linked > 0) {
            log.info("Linked {} seeded account(s) to Supabase Auth", linked);
        }
        return linked;
    }

    static List<String> seedEmails() {
        Set<String> emails = new LinkedHashSet<>(DataSeeder.SEED_EMAILS);
        for (DemoSeedData.DemoStudent student : DemoSeedData.STUDENTS) {
            emails.add(student.email());
        }
        return new ArrayList<>(emails);
    }

    private UUID createOrFind(String email) {
        try {
            return supabase.adminCreateConfirmedUser(email, DataSeeder.SEED_PASSWORD);
        } catch (RuntimeException createFailed) {
            try {
                return supabase.passwordGrant(email, DataSeeder.SEED_PASSWORD).userId();
            } catch (RuntimeException loginFailed) {
                log.warn("Could not link seed account {} to Supabase Auth: {}", email, createFailed.getMessage());
                return null;
            }
        }
    }
}
