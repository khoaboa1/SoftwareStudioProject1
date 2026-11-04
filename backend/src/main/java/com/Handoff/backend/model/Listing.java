package com.Handoff.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "listings")
public class Listing {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String itemName;

  private String description;

  private BigDecimal price;

  @Enumerated(EnumType.STRING)
  private Condition condition;

  @Enumerated(EnumType.STRING)
  private Category category;

  // New listings are immediately browseable unless a future workflow changes their lifecycle.
  @Enumerated(EnumType.STRING)
  private ListingStatus status = ListingStatus.ACTIVE;

  @ManyToOne
  @JoinColumn(name = "seller_id", nullable = false)
  private Student seller;

  private Instant createdAt;

  @Column(name = "image_url", length = 1000)
  private String imageUrl;

  @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position ASC")
  @BatchSize(size = 50)
  private List<ListingImage> images = new ArrayList<>();

  public Listing() {
  }

  public Listing(String itemName, String description, BigDecimal price,
                 Condition condition, Category category, Student seller) {
    this.itemName = itemName;
    this.description = description;
    this.price = price;
    this.condition = condition;
    this.category = category;
    this.status = ListingStatus.ACTIVE;
    this.seller = seller;
    this.createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public String getItemName() {
    return itemName;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public Condition getCondition() {
    return condition;
  }

  public Category getCategory() {
    return category;
  }

  public ListingStatus getStatus() {
    return status;
  }

  public Student getSeller() {
    return seller;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public List<String> getImageUrls() {
    return images.stream().map(ListingImage::getUrl).toList();
  }

  public void replaceImages(List<String> urls) {
    for (int index = 0; index < urls.size(); index++) {
      if (index < images.size()) {
        images.get(index).setUrl(urls.get(index));
      } else {
        images.add(new ListingImage(this, urls.get(index), index));
      }
    }
    if (images.size() > urls.size()) images.subList(urls.size(), images.size()).clear();
    imageUrl = urls.isEmpty() ? null : urls.get(0);
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public void setCondition(Condition condition) {
    this.condition = condition;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public void setStatus(ListingStatus status) {
    this.status = status;
  }

  public void setSeller(Student student) {
    this.seller = student;
  }
}
