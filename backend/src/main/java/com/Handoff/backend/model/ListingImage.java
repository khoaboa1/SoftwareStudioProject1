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

@Entity
@Table(name = "listing_images", uniqueConstraints = @UniqueConstraint(columnNames = {"listing_id", "position"}))
public class ListingImage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "listing_id", nullable = false)
  private Listing listing;

  @Column(nullable = false, length = 1000)
  private String url;

  @Column(nullable = false)
  private int position;

  protected ListingImage() {
  }

  public ListingImage(Listing listing, String url, int position) {
    this.listing = listing;
    this.url = url;
    this.position = position;
  }

  public Long getId() { return id; }
  public Listing getListing() { return listing; }
  public String getUrl() { return url; }
  public void setUrl(String url) { this.url = url; }
  public int getPosition() { return position; }
}
