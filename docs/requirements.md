## Personas (Hudson) — describe the people who will use your app. Each persona has a name, background, and goals.

- Sarah, 18, incoming freshman living on campus. She wants to buy affordable dorm essentials from nearby students and ensure she is safely meeting up with verified peers.

- Alex, 22, graduating senior moving out of state. He wants to sell his used furniture quickly for extra cash and avoid the hassle of hauling bulky items back home.

- David, 19, budget-conscious sophomore moving into his first apartment. He wants to set up automated alerts for specific keywords like "mini-fridge" and find the absolute best deals to stick to his tight budget.

- Chloe, 21, international student flying back home. She wants to bundle all of her small dorm accessories into a single listing and sell them quickly before her flight leaves, without having to manage multiple buyers.

## User stories (Chuong)

1. Create a list of sale items

- User Story: As a graduating senior, I want to quickly post a listing for my desk so that I can sell it before moving out this weekend.

- Acceptance Criteria:
  - AC 1: Given I am logged into my account, when I click "Create Listing" and submit the desk's details (photo, price, description), then the listing is immediately visible on the marketplace feed.

  - AC 2: Given I am creating a listing, when I leave the price field blank and click submit, then the system prevents me from posting and highlights the missing field.

  - AC 3: Given my listing is live, when I view my user profile dashboard, then I see the desk listing and have a button available to mark it as "Sold."

2. Filtering by Location

- User Story: As a freshman living in "Wall Residence Hall", I want to filter items by my specific dorm building so that I don't have to carry heavy purchases across campus.

- Acceptance Criteria:
  - AC 1: Given I am on the marketplace search page, when I select "Wall" from the pickup location filter, then the feed updates to only show active listings located in or near Wall.

  - AC 2: Given I am browsing filtered results, when I click on an item to view its details, then the item should have option pickup location clearly displays "Wall" or nearby with the specific distance.

3. Keyword Alerts

- User Story: As a budget-conscious sophomore, I want to set up an alert for "mini-fridge" so that I get notified the moment a cheap one is posted.

- Acceptance Criteria:
  - AC 1: Given I am on the "Search" page, when I enter the keyword "mini-fridge" and save it, then the items should be appeared in my interested list.

  - AC 2: Given I have an notification for "mini-fridge," when another user posts a listing with "mini-fridge" in the title, then I receive a push notification on my device.

  - AC 3: Given I receive an alert notification, when I tap on it, then the app opens directly to that newly posted item's listing page.

4. Safety and Verification

- User Story: As an international student, I want to see a verification badge on seller profiles so that I know I am meeting up with a real student from my university.

- Acceptance Criteria:
  - AC 1: Given a user has registered and successfully verified their .edu email address, when their profile or listing is displayed, then a "Verified Student" badge appears next to their name.

  - AC 2: Given I am an unverified user, when I attempt to send a direct message to a seller, then the app blocks the action and prompts me to verify my university email first.

## Use cases (Khoa)- describe a specific interaction between a user and the system step by step.

1. Use Case 1 — List an Item for Sale

Actor: Khoa, a student moving out of a dorm

Steps:

1. Khoa logs in to Handoff.
2. Khoa selects the option to create a new listing.
3. Khoa enters the item name, description, price, and uploads a photo.
4. Khoa submits the listing.
5. The system validates the information.
6. The system adds the item to the campus marketplace feed.
7. The system confirms that the listing was successfully created.

8. Use Case 2 — Claim an Item

Actor: Chuong, an incoming freshman

Steps:

1. Chuong logs in to Handoff.
2. Chuong browses the campus marketplace feed.
3. Chuong selects an available item that he wants.
4. Chuong selects the option to request or claim the item..
5. The system sends the claim request to the seller.
6. The system confirms to Chuong that her request was successfully submitted.
7. The seller sees that Chuong has requested the item and can continue arranging the exchange.

## MVP Definition
- In Scope:
1. Student account creation and login,
2. Campus/school verification
3. Create an item listing: Item name, Description, Price, Category, Photo
4. Browse a campus-only marketplace feed
5. View individual item listings
6. Search/filter listings
7. Edit or delete your own listings
8. Request/claim an available item
9. Mark an item as sold/claimed
10. View your own active listings
- Out of Scope:
1. Mobile/iOS/Android app — web app only
2. Shipping or delivery
3. Integrated payments
4. Ratings/reviews
5. Auctions or bidding
6. AI recommendations
7. Facebook Marketplace/Craigslist integration
8. Cross-campus marketplace
9. Advanced seller profiles
10. Push notifications

