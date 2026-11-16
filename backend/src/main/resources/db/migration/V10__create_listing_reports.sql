-- SSP1-115: students report listings; same-school admins review them.
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

-- Required for every new table since V6 (SSP1-109): no access through Supabase's REST API.
alter table listing_reports enable row level security;
