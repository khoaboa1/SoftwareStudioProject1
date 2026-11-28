-- SSP1-159: PIN guesses and PIN sends are counted on the student so a restart does not reset them.
alter table students add column if not exists pin_failed_attempts integer not null default 0;
alter table students add column if not exists pin_last_sent_at timestamp;
alter table students add column if not exists pin_send_window_start timestamp;
alter table students add column if not exists pin_send_count integer not null default 0;
