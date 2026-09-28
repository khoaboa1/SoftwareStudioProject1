package com.Handoff.backend.service;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingRepository;
import com.Handoff.backend.repository.ProfileRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ListingService {

  private final ListingRepository listingRepository;
  private final ProfileRepository profileRepository;

  // Keep tenant resolution in the service so callers cannot supply a domain override.
  public ListingService(ListingRepository listingRepository, ProfileRepository profileRepository) {
    this.listingRepository = listingRepository;
    this.profileRepository = profileRepository;
  }

  /** Resolve the authenticated student's persisted profile before executing a scoped database query. */
  public List<Listing> getFeed(Student requester) {
    var profile = profileRepository.findByStudent_Id(requester.getId())
        .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));
    return listingRepository.findBySchoolDomain(profile.getSchoolDomain());
  }

  public Listing createListing(Student seller, String itemName, String description,
                                BigDecimal price, String conditionRaw, String categoryRaw) {
    if (itemName == null || itemName.isBlank()) {
      throw new InvalidListingException("Item name is required");
    }
    if (description == null || description.isBlank()) {
      throw new InvalidListingException("Description is required");
    }
    if (price == null || price.signum() <= 0) {
      throw new InvalidListingException("Price must be greater than zero");
    }
    Condition condition = parseEnum(Condition.class, conditionRaw, "condition");
    Category category = parseEnum(Category.class, categoryRaw, "category");

    return listingRepository.save(new Listing(itemName, description, price, condition, category, seller));
  }

  public Listing updateListing(Long listingId, Student requester, String itemName, String description,
                                BigDecimal price, String conditionRaw, String categoryRaw) {
    Listing listing = getOwnedListing(listingId, requester);

    if (itemName == null || itemName.isBlank()) {
      throw new InvalidListingException("Item name is required");
    }
    if (description == null || description.isBlank()) {
      throw new InvalidListingException("Description is required");
    }
    if (price == null || price.signum() <= 0) {
      throw new InvalidListingException("Price must be greater than zero");
    }
    Condition condition = parseEnum(Condition.class, conditionRaw, "condition");
    Category category = parseEnum(Category.class, categoryRaw, "category");

    listing.setItemName(itemName);
    listing.setDescription(description);
    listing.setPrice(price);
    listing.setCondition(condition);
    listing.setCategory(category);
    return listingRepository.save(listing);
  }

  public void deleteListing(Long listingId, Student requester) {
    Listing listing = getOwnedListing(listingId, requester);
    listingRepository.delete(listing);
  }

  private Listing getOwnedListing(Long listingId, Student requester) {
    Listing listing = listingRepository.findById(listingId)
        .orElseThrow(ListingNotFoundException::new);
    if (!listing.getSeller().getId().equals(requester.getId())) {
      throw new ForbiddenListingActionException();
    }
    return listing;
  }

  private <E extends Enum<E>> E parseEnum(Class<E> enumType, String raw, String fieldLabel) {
    if (raw == null) {
      throw new InvalidListingException("Invalid " + fieldLabel);
    }
    try {
      return Enum.valueOf(enumType, raw.toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new InvalidListingException("Invalid " + fieldLabel);
    }
  }
}
