package com.Handoff.backend.config;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import java.math.BigDecimal;
import java.util.List;

final class DemoSeedData {
  record DemoStudent(String name, String email, String dormLocation, String major,
                     String bio, String schoolDomain, List<DemoListing> listings) {}

  record DemoListing(String itemName, String description, BigDecimal price,
                     Condition condition, Category category, String imageUrl, int daysAgo) {}

  private static DemoListing item(String name, String description, String price,
                                  Condition condition, Category category, String photoId, int daysAgo) {
    return new DemoListing(name, description, new BigDecimal(price), condition, category,
        "https://images.unsplash.com/photo-" + photoId + "?auto=format&fit=crop&w=800&q=80", daysAgo);
  }

  static final List<DemoStudent> STUDENTS = List.of(
      new DemoStudent("Maya Chen", "maya.chen@tulane.edu", "Wall Residence Hall", "Public Health",
          "I love thrift finds and keeping useful things out of the landfill. Usually studying near the library.",
          "tulane.edu", List.of(
              item("Desk Chair", "Comfortable chair for a small dorm desk.", "45.00", Condition.GOOD, Category.FURNITURE, "1503602642458-232111445657", 1),
              item("Study Lamp", "Adjustable lamp with a warm bulb included.", "18.00", Condition.LIKE_NEW, Category.DECOR, "1507473885765-e6ed057f782c", 2))),
      new DemoStudent("Jordan Rivera", "jordan.rivera@tulane.edu", "Aron Residences", "Computer Science",
          "I am graduating this spring and passing along my apartment setup. Pickup near campus is easy.",
          "tulane.edu", List.of(
              item("24-inch Monitor", "Works well for a second screen; HDMI cable included.", "95.00", Condition.GOOD, Category.ELECTRONICS, "1527443224154-c4a3942d3acf", 3),
              item("Bookshelf", "Compact wood shelf that fits beside a desk.", "35.00", Condition.FAIR, Category.FURNITURE, "1594620302200-9a762244a156", 4))),
      new DemoStudent("Avery Patel", "avery.patel@tulane.edu", "Weatherhead Hall", "Architecture",
          "I sketch buildings and collect secondhand decor. Happy to meet on the Uptown campus.",
          "tulane.edu", List.of(
              item("Area Rug", "Neutral rug, cleaned and ready for a new room.", "60.00", Condition.GOOD, Category.DECOR, "1600166898405-da9535204843", 5),
              item("Art Print Set", "Three framed prints for a dorm wall.", "24.50", Condition.LIKE_NEW, Category.DECOR, "1549490349-8643362247b5", 6))),
      new DemoStudent("Olivia Brooks", "olivia.brooks@tulane.edu", "Josephine Louise Hall", "Biology",
          "Lab days keep me busy, but I can arrange evening pickup. I take good care of my things.",
          "tulane.edu", List.of(
              item("Textbook Bundle", "Biology and chemistry reference books with clean pages.", "40.00", Condition.FAIR, Category.OTHER, "1497633762265-9d179a990aa6", 7),
              item("Coffee Maker", "Small drip coffee maker, tested and descaled.", "22.00", Condition.GOOD, Category.KITCHEN, "1495474472287-4d71bcdd2085", 8))),
      new DemoStudent("Noah Williams", "noah.williams@tulane.edu", "Greenbaum House", "Economics",
          "Moving out of my dorm and selling the things I cannot take home. Message me for pickup times.",
          "tulane.edu", List.of(
              item("Mini Fridge", "Dorm-size fridge, clean and cold.", "110.00", Condition.GOOD, Category.KITCHEN, "1571175443880-49e1d25b2bc5", 9),
              item("Microwave", "Compact microwave with a rotating plate.", "55.00", Condition.WORN, Category.KITCHEN, "1574269909862-7e1d70bb8078", 10))),
      new DemoStudent("Sofia Martinez", "sofia.martinez@tulane.edu", "Sharp Hall", "English",
          "I read a lot and decorate every room I live in. Everything listed is ready for campus pickup.",
          "tulane.edu", List.of(
              item("Denim Jacket", "Classic blue jacket, size medium.", "30.00", Condition.LIKE_NEW, Category.CLOTHING, "1544022613-e87ca75a784a", 11),
              item("Desk Organizer", "Wooden organizer for pens, notes and small supplies.", "12.00", Condition.NEW, Category.OTHER, "1533090161767-e6ffed986c88", 12))),
      new DemoStudent("Ethan Cooper", "ethan.cooper@usf.edu", "Juniper-Poplar Hall", "Mechanical Engineering",
          "I bike to class and like fixing up useful gear. I can meet near Juniper-Poplar.",
          "usf.edu", List.of(
              item("Campus Bicycle", "Reliable commuter bike with a lock.", "180.00", Condition.GOOD, Category.OTHER, "1485965120184-e220f721d03e", 13),
              item("Wireless Headphones", "Over-ear headphones with charging cable.", "65.00", Condition.LIKE_NEW, Category.ELECTRONICS, "1505740420928-5e560c06d30e", 14))),
      new DemoStudent("Priya Shah", "priya.shah@usf.edu", "The Village", "Psychology",
          "I enjoy cozy spaces and good coffee. My listings come from a smoke-free room.",
          "usf.edu", List.of(
              item("Throw Blanket", "Soft washable blanket in a cream color.", "20.00", Condition.GOOD, Category.DECOR, "1600210492486-724fe5c67fb0", 15),
              item("Electric Kettle", "Fast-boiling kettle with automatic shutoff.", "28.00", Condition.NEW, Category.KITCHEN, "1498804103079-a6351b050096", 16))),
      new DemoStudent("Liam Thompson", "liam.thompson@ucf.edu", "Neptune Community", "Digital Media",
          "I make videos and keep my gear in good shape. Pickup near Neptune works best.",
          "ucf.edu", List.of(
              item("32-inch TV", "Clear picture and remote included.", "120.00", Condition.GOOD, Category.ELECTRONICS, "1593359677879-a4bb92f829d1", 17),
              item("Camera Tripod", "Lightweight tripod with carrying bag.", "32.00", Condition.FAIR, Category.ELECTRONICS, "1526170375885-4d8ecf77b99f", 18))),
      new DemoStudent("Isabella Nguyen", "isabella.nguyen@ucf.edu", "Towers at Knights Plaza", "Nursing",
          "I am clearing space before clinical rotations. All items can be picked up on campus.",
          "ucf.edu", List.of(
              item("Storage Baskets", "Set of two woven baskets for closet storage.", "16.00", Condition.GOOD, Category.FURNITURE, "1586023492125-27b2c045efd7", 19),
              item("Scrub Jacket", "Lightweight zip jacket, size small.", "25.00", Condition.WORN, Category.CLOTHING, "1551028719-00167b16eac5", 20))));

  private DemoSeedData() {}
}
