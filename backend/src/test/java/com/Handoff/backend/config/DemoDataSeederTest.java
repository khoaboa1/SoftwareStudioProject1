package com.Handoff.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingRepository;
import com.Handoff.backend.repository.ProfileRepository;
import com.Handoff.backend.repository.StudentRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("dev")
@Import({DemoDataSeeder.class, DemoDataSeederTest.EncoderConfig.class})
class DemoDataSeederTest {
  @TestConfiguration
  static class EncoderConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
  }

  @Autowired DemoDataSeeder seeder;
  @Autowired StudentRepository students;
  @Autowired ProfileRepository profiles;
  @Autowired ListingRepository listings;
  @Autowired PasswordEncoder passwordEncoder;

  @BeforeEach
  void clearStartupSeedData() {
    listings.deleteAllInBatch();
    profiles.deleteAllInBatch();
    students.deleteAllInBatch();
  }

  @Test
  void run_seedsTenStudentsTwentyListings() throws Exception {
    seeder.run();
    assertThat(students.count()).isEqualTo(10);
    assertThat(profiles.count()).isEqualTo(10);
    assertThat(listings.count()).isEqualTo(20);
  }

  @Test
  void run_splitsSchools_6_2_2_andListings_12_4_4() throws Exception {
    seeder.run();
    assertThat(profiles.findAll().stream().collect(Collectors.groupingBy(
        profile -> profile.getSchoolDomain(), Collectors.counting())))
        .containsEntry("tulane.edu", 6L).containsEntry("usf.edu", 2L).containsEntry("ucf.edu", 2L);
    assertThat(listings.findAll().stream().collect(Collectors.groupingBy(
        listing -> profiles.findByStudent_Id(listing.getSeller().getId()).orElseThrow().getSchoolDomain(),
        Collectors.counting())))
        .containsEntry("tulane.edu", 12L).containsEntry("usf.edu", 4L).containsEntry("ucf.edu", 4L);
  }

  @Test
  void run_everyListingHasHttpsUnsplashImage() throws Exception {
    seeder.run();
    assertThat(listings.findAll()).allSatisfy(listing ->
        assertThat(listing.getImageUrl()).startsWith("https://images.unsplash.com/"));
  }

  @Test
  void run_seededStudentsAreVerifiedAndCanLogIn() throws Exception {
    seeder.run();
    assertThat(students.findAll()).allSatisfy(student -> {
      assertThat(student.isVerified()).isTrue();
      assertThat(student.isEmailVerified()).isTrue();
      assertThat(passwordEncoder.matches("Password123!", student.getPasswordHash())).isTrue();
    });
  }

  @Test
  void run_isIdempotent_whenRunTwice() throws Exception {
    seeder.run();
    seeder.run();
    assertThat(students.count()).isEqualTo(10);
    assertThat(profiles.count()).isEqualTo(10);
    assertThat(listings.count()).isEqualTo(20);
  }

  @Test
  void run_skipsStudentWhoseEmailAlreadyExists() throws Exception {
    Student existing = students.save(new Student("Existing Student", DemoSeedData.STUDENTS.getFirst().email(),
        "different-hash", null, "Wall Residence Hall"));
    seeder.run();
    assertThat(students.findByEmail(existing.getEmail()).orElseThrow().getPasswordHash()).isEqualTo("different-hash");
    assertThat(profiles.findByStudent_Id(existing.getId())).isEmpty();
    assertThat(listings.findAll()).noneMatch(listing -> listing.getSeller().getId().equals(existing.getId()));
    assertThat(students.count()).isEqualTo(10);
    assertThat(profiles.count()).isEqualTo(9);
    assertThat(listings.count()).isEqualTo(18);
  }

  @Test
  void seeder_isRestrictedToDevProfile() {
    assertThat(DemoDataSeeder.class.getAnnotation(Profile.class).value()).containsExactly("dev");
  }

  @Test
  void demoData_matchesDataRules() {
    List<DemoSeedData.DemoStudent> data = DemoSeedData.STUDENTS;
    List<DemoSeedData.DemoListing> items = data.stream().flatMap(student -> student.listings().stream()).toList();
    assertThat(data).hasSize(10).allSatisfy(student -> assertThat(student.listings()).isNotEmpty());
    assertThat(data.stream().map(DemoSeedData.DemoStudent::email)).doesNotHaveDuplicates();
    assertThat(items).hasSize(20).allSatisfy(item -> {
      assertThat(item.price()).isBetween(new BigDecimal("5"), new BigDecimal("400"));
      assertThat(item.imageUrl()).startsWith("https://images.unsplash.com/photo-");
    });
    assertThat(items.stream().map(DemoSeedData.DemoListing::daysAgo)).doesNotHaveDuplicates();
    assertThat(items.stream().map(DemoSeedData.DemoListing::category).collect(Collectors.toSet()))
        .isEqualTo(Set.of(Category.values()));
    assertThat(items.stream().map(DemoSeedData.DemoListing::condition).collect(Collectors.toSet()))
        .isEqualTo(Set.of(Condition.values()));
  }
}
