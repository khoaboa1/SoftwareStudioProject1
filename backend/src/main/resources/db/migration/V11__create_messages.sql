-- SSP1-135: messages exchanged between the buyer and the seller of a conversation.
create table messages (
  id bigserial primary key,
  conversation_id bigint not null,
  sender_id bigint not null,
  content varchar(2000) not null,
  created_at timestamp(6) with time zone not null,
  constraint fk_messages_conversation foreign key (conversation_id) references conversations (id) on delete cascade,
  constraint fk_messages_sender foreign key (sender_id) references students (id)
);

create index idx_messages_conversation_created on messages (conversation_id, created_at);

-- Required for every new table since V6 (SSP1-109): no access through Supabase's REST API.
alter table messages enable row level security;
