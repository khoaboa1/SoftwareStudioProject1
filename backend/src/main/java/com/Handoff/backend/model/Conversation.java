package com.Handoff.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "conversations", uniqueConstraints = @UniqueConstraint(columnNames = {"listing_id", "buyer_id"}))
public class Conversation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "listing_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Listing listing;

  @ManyToOne(optional = false)
  @JoinColumn(name = "buyer_id", nullable = false)
  private Student buyer;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Conversation() {
  }

  public Conversation(Listing listing, Student buyer) {
    this.listing = listing;
    this.buyer = buyer;
    this.createdAt = Instant.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Listing getListing() { return listing; }
  public Student getBuyer() { return buyer; }
  public Instant getCreatedAt() { return createdAt; }
}
