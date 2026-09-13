package com.Handoff.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

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

  @ManyToOne
  @JoinColumn(name = "seller_id", nullable = false)
  private Student seller;

  private Instant createdAt;

  public Listing() {
  }

  public Listing(String itemName, String description, BigDecimal price,
                 Condition condition, Category category, Student seller) {
    this.itemName = itemName;
    this.description = description;
    this.price = price;
    this.condition = condition;
    this.category = category;
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

  public Student getSeller() {
    return seller;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
