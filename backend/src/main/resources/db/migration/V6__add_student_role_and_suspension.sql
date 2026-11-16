alter table students add column role varchar(20) not null default 'STUDENT';
alter table students add column suspended boolean not null default false;
