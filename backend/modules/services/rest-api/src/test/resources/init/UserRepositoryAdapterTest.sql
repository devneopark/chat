insert into users (
  id,
  principal,
  password_hash,
  display_name,
  registered_at
) values (
  'seed-user-001',
  'existing.principal',
  'seed-hashed-password',
  'Seed User',
  current_timestamp
);

insert into users (
  id,
  principal,
  password_hash,
  display_name,
  registered_at,
  withdrawn_at
) values (
  'withdrawn-user-001',
  'withdrawn.principal',
  'withdrawn-hashed-password',
  'Withdrawn User',
  current_timestamp,
  current_timestamp
);
