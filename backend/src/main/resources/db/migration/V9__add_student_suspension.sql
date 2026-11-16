-- SSP1-115: suspended students cannot log in and their listings are hidden.
-- The role column already exists from V8 (SSP1-114).
alter table students add column if not exists suspended boolean not null default false;
