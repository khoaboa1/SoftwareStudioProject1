package com.Handoff.backend.service;

import com.Handoff.backend.model.Conversation;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.ListingStatus;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ConversationRepository;
import com.Handoff.backend.repository.ProfileRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {
  private final ListingService listingService;
  private final ConversationRepository conversationRepository;
  private final ProfileRepository profileRepository;

  public record StartResult(Conversation conversation, boolean created) {}

  public ConversationService(ListingService listingService, ConversationRepository conversationRepository,
      ProfileRepository profileRepository) {
    this.listingService = listingService;
    this.conversationRepository = conversationRepository;
    this.profileRepository = profileRepository;
  }

  public StartResult startOrReopen(Long listingId, Student buyer) {
    Listing listing = listingService.getListing(listingId, buyer);
    if (listing.getSeller().getId().equals(buyer.getId())) {
      throw new OwnListingConversationException();
    }

    var existing = conversationRepository.findByListing_IdAndBuyer_Id(listingId, buyer.getId());
    if (existing.isPresent()) {
      return new StartResult(existing.get(), false);
    }
    if (listing.getStatus() == ListingStatus.SOLD) {
      throw new ListingSoldException();
    }
    try {
      return new StartResult(conversationRepository.saveAndFlush(new Conversation(listing, buyer)), true);
    } catch (DataIntegrityViolationException ex) {
      return conversationRepository.findByListing_IdAndBuyer_Id(listingId, buyer.getId())
          .map(conversation -> new StartResult(conversation, false))
          .orElseThrow(() -> ex);
    }
  }

  public List<Conversation> listForStudent(Student student) {
    requireProfile(student);
    return conversationRepository.findAllForStudent(student.getId());
  }

  public Conversation getForParticipant(Long conversationId, Student student) {
    requireProfile(student);
    Conversation conversation = conversationRepository.findWithParticipantsById(conversationId)
        .orElseThrow(ConversationNotFoundException::new);
    requireParticipant(conversation, student);
    return conversation;
  }

  public void requireParticipant(Conversation conversation, Student student) {
    if (!Objects.equals(student.getId(), conversation.getBuyer().getId())
        && !Objects.equals(student.getId(), conversation.getListing().getSeller().getId())) {
      throw new ConversationAccessDeniedException();
    }
  }

  private void requireProfile(Student student) {
    profileRepository.findByStudent_Id(student.getId())
        .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));
  }
}
