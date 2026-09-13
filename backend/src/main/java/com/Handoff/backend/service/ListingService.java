package com.Handoff.backend.service;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ListingService {

  private final ListingRepository listingRepository;

  public ListingService(ListingRepository listingRepository) {
    this.listingRepository = listingRepository;
  }

  public List<Listing> getFeed() {
    return listingRepository.findAllByOrderByCreatedAtDesc();
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
