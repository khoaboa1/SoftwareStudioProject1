create table listing_images (
  id bigserial primary key,
  listing_id bigint not null,
  url varchar(1000) not null,
  position integer not null,
  constraint fk_listing_images_listing foreign key (listing_id) references listings (id) on delete cascade,
  constraint uq_listing_images_position unique (listing_id, position)
);

create index idx_listing_images_listing on listing_images (listing_id);

insert into listing_images (listing_id, url, position)
select id, image_url, 0 from listings where image_url is not null;
