-- SSP1-116: link each student to their Supabase Auth user (the JWT "sub" claim).
alter table students add column if not exists supabase_user_id uuid;
create unique index if not exists ux_students_supabase_user_id on students (supabase_user_id);
