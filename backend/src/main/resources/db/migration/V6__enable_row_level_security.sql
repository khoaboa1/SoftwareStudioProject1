-- SSP1-109: Lock down the public tables on Supabase.
--
-- Supabase exposes every table in the public schema through its REST API
-- (PostgREST) using the anon/authenticated roles. Enabling Row Level Security
-- with no policies denies those roles all access.
--
-- The backend is unaffected: it connects as the table owner (postgres), which
-- bypasses RLS. If the backend ever uses a different DB role, that role needs
-- BYPASSRLS or explicit policies.
--
-- Future migrations that create tables must enable RLS in the same file.

alter table students enable row level security;
alter table profiles enable row level security;
alter table listings enable row level security;
alter table student_trusted_devices enable row level security;
alter table student_selling_items enable row level security;
alter table listing_images enable row level security;

alter table conversations enable row level security;

-- NOTE: flyway_schema_history also lives in public, but it is intentionally not
-- altered here. Flyway holds locks on it while a migration runs, so altering it
-- from inside a migration times out. Enable RLS on it once, by hand, in the
-- Supabase SQL Editor:
--   alter table public.flyway_schema_history enable row level security;

-- Defense in depth: also remove direct privileges from the REST API roles.
-- Guarded so the migration still runs on plain PostgreSQL (e.g. local Docker),
-- where the Supabase roles do not exist.
do $$
declare
    api_role text;
begin
    foreach api_role in array array['anon', 'authenticated'] loop
        if exists (select 1 from pg_roles where rolname = api_role) then
            execute format('revoke all on all tables in schema public from %I', api_role);
            execute format('revoke all on all sequences in schema public from %I', api_role);
        end if;
    end loop;
end
$$;
