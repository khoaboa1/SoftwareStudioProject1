create table conversations (
  id bigserial primary key,
  listing_id bigint not null,
  buyer_id bigint not null,
  created_at timestamp(6) with time zone not null,
  constraint fk_conversations_listing foreign key (listing_id) references listings (id) on delete cascade,
  constraint fk_conversations_buyer foreign key (buyer_id) references students (id),
  constraint uq_conversations_listing_buyer unique (listing_id, buyer_id)
);

create index idx_conversations_buyer on conversations (buyer_id);
