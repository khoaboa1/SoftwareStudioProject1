package com.Handoff.backend.service;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

  @Mock
  private ListingRepository listingRepository;

  private Student newStudent(long id, String name) {
    Student student = new Student(name, name.toLowerCase() + "@tulane.edu", "hashed", null, null);
    student.setId(id);
    return student;
  }

  private Listing newListing(long id, Student seller) {
    Listing listing = new Listing("Desk Lamp", "Works great", new BigDecimal("10.00"),
        Condition.GOOD, Category.FURNITURE, seller);
    listing.setId(id);
    return listing;
  }

  @Test
  void updateListing_ownerProvidesValidData_savesUpdatedFields() {
    // Arrange
    Student owner = newStudent(1L, "Jane");
    Listing existing = newListing(10L, owner);
    when(listingRepository.findById(10L)).thenReturn(Optional.of(existing));
    when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));
    ListingService service = new ListingService(listingRepository);

    // Act
    Listing updated = service.updateListing(10L, owner, "Mini Fridge", "Barely used",
        new BigDecimal("40.00"), "LIKE_NEW", "KITCHEN");

    // Assert
    assertThat(updated.getItemName()).isEqualTo("Mini Fridge");
    assertThat(updated.getDescription()).isEqualTo("Barely used");
    assertThat(updated.getPrice()).isEqualByComparingTo("40.00");
    assertThat(updated.getCondition()).isEqualTo(Condition.LIKE_NEW);
    assertThat(updated.getCategory()).isEqualTo(Category.KITCHEN);
  }

  @Test
  void updateListing_listingDoesNotExist_throwsListingNotFound() {
    // Arrange
    Student requester = newStudent(1L, "Jane");
    when(listingRepository.findById(99L)).thenReturn(Optional.empty());
    ListingService service = new ListingService(listingRepository);

    // Act & Assert
    assertThatThrownBy(() -> service.updateListing(99L, requester, "Desk Lamp", "Works great",
        new BigDecimal("10.00"), "GOOD", "FURNITURE"))
        .isInstanceOf(ListingNotFoundException.class);
  }

  @Test
  void updateListing_requesterIsNotSeller_throwsForbidden() {
    // Arrange
    Student owner = newStudent(1L, "Jane");
    Student otherStudent = newStudent(2L, "Bob");
    Listing existing = newListing(10L, owner);
    when(listingRepository.findById(10L)).thenReturn(Optional.of(existing));
    ListingService service = new ListingService(listingRepository);

    // Act & Assert
    assertThatThrownBy(() -> service.updateListing(10L, otherStudent, "Mini Fridge", "Barely used",
        new BigDecimal("40.00"), "LIKE_NEW", "KITCHEN"))
        .isInstanceOf(ForbiddenListingActionException.class);
  }

  @Test
  void updateListing_nonPositivePrice_throwsInvalidListing() {
    // Arrange
    Student owner = newStudent(1L, "Jane");
    Listing existing = newListing(10L, owner);
    when(listingRepository.findById(10L)).thenReturn(Optional.of(existing));
    ListingService service = new ListingService(listingRepository);

    // Act & Assert
    assertThatThrownBy(() -> service.updateListing(10L, owner, "Desk Lamp", "Works great",
        new BigDecimal("0"), "GOOD", "FURNITURE"))
        .isInstanceOf(InvalidListingException.class);
  }

  @Test
  void deleteListing_owner_deletesListing() {
    // Arrange
    Student owner = newStudent(1L, "Jane");
    Listing existing = newListing(10L, owner);
    when(listingRepository.findById(10L)).thenReturn(Optional.of(existing));
    ListingService service = new ListingService(listingRepository);

    // Act
    service.deleteListing(10L, owner);

    // Assert
    verify(listingRepository).delete(existing);
  }

  @Test
  void deleteListing_listingDoesNotExist_throwsListingNotFound() {
    // Arrange
    Student requester = newStudent(1L, "Jane");
    when(listingRepository.findById(99L)).thenReturn(Optional.empty());
    ListingService service = new ListingService(listingRepository);

    // Act & Assert
    assertThatThrownBy(() -> service.deleteListing(99L, requester))
        .isInstanceOf(ListingNotFoundException.class);
    verify(listingRepository, never()).delete(any(Listing.class));
  }

  @Test
  void deleteListing_requesterIsNotSeller_throwsForbidden() {
    // Arrange
    Student owner = newStudent(1L, "Jane");
    Student otherStudent = newStudent(2L, "Bob");
    Listing existing = newListing(10L, owner);
    when(listingRepository.findById(10L)).thenReturn(Optional.of(existing));
    ListingService service = new ListingService(listingRepository);

    // Act & Assert
    assertThatThrownBy(() -> service.deleteListing(10L, otherStudent))
        .isInstanceOf(ForbiddenListingActionException.class);
    verify(listingRepository, never()).delete(any(Listing.class));
  }
}
