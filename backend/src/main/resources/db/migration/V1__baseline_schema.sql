-- Baseline schema owned by Flyway.
-- Hibernate uses ddl-auto=validate and will not create or alter tables.
-- All column names use physical snake_case to match JPA @Column mappings.

create table if not exists students (
    id bigserial primary key,
    student_name varchar(255),
    email varchar(255) unique not null,
    password_hash varchar(255),
    oauth_provider varchar(255),
    email_verified boolean not null default false,
    verified boolean not null default false,
    created_at timestamp,
    verification_pin varchar(255),
    verification_pin_expiry timestamp,
    dorm_location varchar(255)
);

create table if not exists profiles (
    id bigserial primary key,
    student_id bigint not null unique,
    name varchar(255) not null,
    major varchar(255) not null,
    bio varchar(1000),
    school_domain varchar(255) not null,
    created_at timestamp not null,
    constraint fk_profiles_student foreign key (student_id) references students (id)
);

create table if not exists listings (
    id bigserial primary key,
    item_name varchar(255),
    description varchar(255),
    price numeric(19,2),
    condition varchar(255),
    category varchar(255),
    status varchar(255),
    seller_id bigint not null,
    created_at timestamp,
    constraint fk_listings_seller foreign key (seller_id) references students (id)
);

create table if not exists student_trusted_devices (
    student_id bigint not null,
    device_id varchar(255),
    constraint fk_trusted_devices_student foreign key (student_id) references students (id)
);

create table if not exists student_selling_items (
    student_id bigint not null,
    item varchar(255),
    constraint fk_selling_items_student foreign key (student_id) references students (id)
);
