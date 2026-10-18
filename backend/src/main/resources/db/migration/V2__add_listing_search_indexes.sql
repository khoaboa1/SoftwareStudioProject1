-- Enable pg_trgm extension for trigram-based text search
create extension if not exists pg_trgm;

-- Indexes for filter/sort/join columns used by the browse query
create index if not exists idx_listings_status on listings (status);
create index if not exists idx_listings_category on listings (category);
create index if not exists idx_listings_price on listings (price);
create index if not exists idx_listings_created_at on listings (created_at);
create index if not exists idx_listings_seller_id on listings (seller_id);
create index if not exists idx_profiles_student_school_domain on profiles (student_id, school_domain);

-- Trigram GIN indexes compatible with LOWER(column) LIKE '%term%' predicates
create index if not exists idx_listings_item_name_trgm on listings using gin (lower(item_name) gin_trgm_ops);
create index if not exists idx_listings_description_trgm on listings using gin (lower(description) gin_trgm_ops);
