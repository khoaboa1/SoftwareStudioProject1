package com.Handoff.backend.config;

import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingRepository;
import com.Handoff.backend.repository.ProfileRepository;
import com.Handoff.backend.repository.StudentRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DemoDataSeeder implements CommandLineRunner {
  private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
  private final StudentRepository students;
  private final ProfileRepository profiles;
  private final ListingRepository listings;
  private final PasswordEncoder passwordEncoder;

  public DemoDataSeeder(StudentRepository students, ProfileRepository profiles,
                        ListingRepository listings, PasswordEncoder passwordEncoder) {
    this.students = students;
    this.profiles = profiles;
    this.listings = listings;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    String passwordHash = passwordEncoder.encode(DataSeeder.SEED_PASSWORD);
    int created = 0;
    int skipped = 0;
    for (DemoSeedData.DemoStudent data : DemoSeedData.STUDENTS) {
      if (students.findByEmail(data.email()).isPresent()) {
        skipped++;
        continue;
      }
      Student student = new Student(data.name(), data.email(), passwordHash, null, data.dormLocation());
      student.setVerified(true);
      student.setEmailVerified(true);
      student = students.save(student);
      profiles.save(new com.Handoff.backend.model.Profile(student, data.name(), data.major(), data.bio(), data.schoolDomain()));
      for (DemoSeedData.DemoListing item : data.listings()) {
        Listing listing = new Listing(item.itemName(), item.description(), item.price(),
            item.condition(), item.category(), student);
        listing.setImageUrl(item.imageUrl());
        listing.setCreatedAt(Instant.now().minus(item.daysAgo(), ChronoUnit.DAYS));
        listings.save(listing);
      }
      created++;
    }
    log.info("Demo data: created {} students, skipped {} existing students", created, skipped);
  }
}
