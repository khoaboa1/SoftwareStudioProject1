create table listing_reports (
  id bigserial primary key,
  listing_id bigint not null references listings(id) on delete cascade,
  reporter_id bigint not null references students(id),
  reason varchar(30) not null,
  details varchar(500),
  status varchar(20) not null default 'OPEN',
  created_at timestamp(6) with time zone not null
);
create index idx_listing_reports_status_listing on listing_reports (status, listing_id);
