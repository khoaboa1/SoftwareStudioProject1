package com.Handoff.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Conversation;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.ListingStatus;
import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ConversationRepository;
import com.Handoff.backend.repository.ProfileRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {
  @Mock private ListingService listingService;
  @Mock private ConversationRepository conversationRepository;
  @Mock private ProfileRepository profileRepository;
  private Student buyer;
  private Listing listing;

  @BeforeEach
  void setUp() {
    buyer = student(1L, "Buyer");
    listing = new Listing("Lamp", "Works", BigDecimal.TEN, Condition.GOOD,
        Category.FURNITURE, student(2L, "Seller"));
    listing.setId(10L);
  }

  private Student student(long id, String name) {
    Student student = new Student(name, name.toLowerCase() + "@tulane.edu", "hash", null, null);
    student.setId(id);
    return student;
  }

  private ConversationService service() {
    return new ConversationService(listingService, conversationRepository, profileRepository);
  }

  private void visible() {
    when(listingService.getListing(10L, buyer)).thenReturn(listing);
  }

  private void savesConversation() {
    when(conversationRepository.saveAndFlush(any(Conversation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void startOrReopen_newConversation_isCreated() {
    visible();
    savesConversation();
    var result = service().startOrReopen(10L, buyer);
    assertThat(result.created()).isTrue();
    assertThat(result.conversation().getListing()).isSameAs(listing);
    assertThat(result.conversation().getBuyer()).isSameAs(buyer);
  }

  @Test
  void startOrReopen_existingConversation_isReturnedNotDuplicated() {
    visible();
    Conversation existing = new Conversation(listing, buyer);
    when(conversationRepository.findByListing_IdAndBuyer_Id(10L, 1L)).thenReturn(Optional.of(existing));
    var result = service().startOrReopen(10L, buyer);
    assertThat(result.created()).isFalse();
    assertThat(result.conversation()).isSameAs(existing);
    verify(conversationRepository, never()).saveAndFlush(any());
  }

  @Test
  void startOrReopen_ownListing_throwsForbidden() {
    listing.setSeller(buyer);
    visible();
    assertThatThrownBy(() -> service().startOrReopen(10L, buyer))
        .isInstanceOf(OwnListingConversationException.class)
        .hasMessage("You cannot message yourself about your own listing");
    verify(conversationRepository, never()).saveAndFlush(any());
  }

  @Test
  void startOrReopen_soldListing_throwsConflict() {
    listing.setStatus(ListingStatus.SOLD);
    visible();
    assertThatThrownBy(() -> service().startOrReopen(10L, buyer))
        .isInstanceOf(ListingSoldException.class).hasMessage("This item has already been sold");
  }

  @Test
  void startOrReopen_existingConversationOnSoldListing_isReturned() {
    listing.setStatus(ListingStatus.SOLD);
    visible();
    Conversation existing = new Conversation(listing, buyer);
    when(conversationRepository.findByListing_IdAndBuyer_Id(10L, 1L)).thenReturn(Optional.of(existing));
    assertThat(service().startOrReopen(10L, buyer).created()).isFalse();
  }

  @Test
  void startOrReopen_pendingListing_creates() {
    listing.setStatus(ListingStatus.PENDING);
    visible();
    savesConversation();
    assertThat(service().startOrReopen(10L, buyer).created()).isTrue();
  }

  @Test
  void startOrReopen_nullStatus_creates() {
    listing.setStatus(null);
    visible();
    savesConversation();
    assertThat(service().startOrReopen(10L, buyer).created()).isTrue();
  }

  @Test
  void startOrReopen_listingNotVisible_propagatesNotFound() {
    when(listingService.getListing(10L, buyer)).thenThrow(new ListingNotFoundException());
    assertThatThrownBy(() -> service().startOrReopen(10L, buyer))
        .isInstanceOf(ListingNotFoundException.class);
  }

  @Test
  void startOrReopen_saveHitsUniqueConstraint_returnsExisting() {
    visible();
    Conversation existing = new Conversation(listing, buyer);
    when(conversationRepository.findByListing_IdAndBuyer_Id(10L, 1L))
        .thenReturn(Optional.empty(), Optional.of(existing));
    when(conversationRepository.saveAndFlush(any(Conversation.class)))
        .thenThrow(new DataIntegrityViolationException("unique"));
    var result = service().startOrReopen(10L, buyer);
    assertThat(result.created()).isFalse();
    assertThat(result.conversation()).isSameAs(existing);
  }

  private void hasProfile(Student student) {
    when(profileRepository.findByStudent_Id(student.getId()))
        .thenReturn(Optional.of(new Profile(student, student.getStudentName(), "CS", null, "tulane.edu")));
  }

  private void hasNoProfile(Student student) {
    when(profileRepository.findByStudent_Id(student.getId())).thenReturn(Optional.empty());
  }

  private Conversation conversation(long id) {
    Conversation conversation = new Conversation(listing, buyer);
    conversation.setId(id);
    return conversation;
  }

  @Test
  void listForStudent_returnsRepositoryResults() {
    hasProfile(buyer);
    Conversation conversation = conversation(5L);
    when(conversationRepository.findAllForStudent(1L)).thenReturn(List.of(conversation));
    assertThat(service().listForStudent(buyer)).containsExactly(conversation);
  }

  @Test
  void listForStudent_withoutProfile_throwsProfileNotFound() {
    hasNoProfile(buyer);
    assertThatThrownBy(() -> service().listForStudent(buyer))
        .isInstanceOf(ProfileNotFoundException.class).hasMessage("Profile not found");
    verify(conversationRepository, never()).findAllForStudent(any());
  }

  @Test
  void getForParticipant_buyer_returnsConversation() {
    hasProfile(buyer);
    Conversation conversation = conversation(5L);
    when(conversationRepository.findWithParticipantsById(5L)).thenReturn(Optional.of(conversation));
    assertThat(service().getForParticipant(5L, buyer)).isSameAs(conversation);
  }

  @Test
  void getForParticipant_seller_returnsConversation() {
    Student seller = listing.getSeller();
    hasProfile(seller);
    Conversation conversation = conversation(5L);
    when(conversationRepository.findWithParticipantsById(5L)).thenReturn(Optional.of(conversation));
    assertThat(service().getForParticipant(5L, seller)).isSameAs(conversation);
  }

  @Test
  void getForParticipant_soldListing_stillReturned() {
    listing.setStatus(ListingStatus.SOLD);
    hasProfile(buyer);
    Conversation conversation = conversation(5L);
    when(conversationRepository.findWithParticipantsById(5L)).thenReturn(Optional.of(conversation));
    assertThat(service().getForParticipant(5L, buyer)).isSameAs(conversation);
  }

  @Test
  void getForParticipant_nonParticipant_throwsAccessDenied() {
    Student other = student(3L, "Other");
    hasProfile(other);
    when(conversationRepository.findWithParticipantsById(5L)).thenReturn(Optional.of(conversation(5L)));
    assertThatThrownBy(() -> service().getForParticipant(5L, other))
        .isInstanceOf(ConversationAccessDeniedException.class)
        .hasMessage("You do not have access to this conversation");
  }

  @Test
  void getForParticipant_missing_throwsNotFound() {
    hasProfile(buyer);
    when(conversationRepository.findWithParticipantsById(99L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service().getForParticipant(99L, buyer))
        .isInstanceOf(ConversationNotFoundException.class)
        .hasMessage("Conversation not found");
  }

  @Test
  void getForParticipant_withoutProfile_throwsProfileNotFound() {
    hasNoProfile(buyer);
    assertThatThrownBy(() -> service().getForParticipant(5L, buyer))
        .isInstanceOf(ProfileNotFoundException.class).hasMessage("Profile not found");
    verify(conversationRepository, never()).findWithParticipantsById(any());
  }

  @Test
  void requireParticipant_nonParticipant_throwsAccessDenied() {
    assertThatThrownBy(() -> service().requireParticipant(conversation(5L), student(3L, "Other")))
        .isInstanceOf(ConversationAccessDeniedException.class);
  }
}
