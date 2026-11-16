-- SSP1-114: add role column to students table
alter table students add column if not exists role varchar(50) not null default 'STUDENT' check (role in ('STUDENT', 'ADMIN'));
