insert into users (
  id,
  principal,
  password_hash,
  display_name,
  registered_at
) values (
  'user-context-find-001',
  'user.context.find.principal',
  'find-hashed-password',
  'Seed User',
  current_timestamp
);

insert into users (
  id,
  principal,
  password_hash,
  display_name,
  registered_at
) values (
  'user-context-update-001',
  'user.context.update.principal',
  'update-hashed-password',
  'Update User',
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
  'user-context-withdrawn-001',
  'withdrawn.context.principal',
  'withdrawn-hashed-password',
  'Withdrawn User',
  current_timestamp,
  current_timestamp
);
