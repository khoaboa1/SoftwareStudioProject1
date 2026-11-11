package com.Handoff.backend.service;

import com.Handoff.backend.model.Conversation;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.ListingStatus;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ConversationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {
  private final ListingService listingService;
  private final ConversationRepository conversationRepository;

  public record StartResult(Conversation conversation, boolean created) {}

  public ConversationService(ListingService listingService, ConversationRepository conversationRepository) {
    this.listingService = listingService;
    this.conversationRepository = conversationRepository;
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
}
