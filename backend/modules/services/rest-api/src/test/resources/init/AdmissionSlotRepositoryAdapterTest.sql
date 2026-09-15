insert into users(
    id,
    principal,
    password_hash,
    display_name
) values (
    'slot-user-001',
    'slot.seed.principal',
    'seed-hashed-password',
    'Slot Seed User'
);

insert into room(
    id,
    title
) values (
    'slot-room-001',
    'Slot Seed Room'
);

insert into participant(
    id,
    room_id,
    user_id,
    participant_role,
    joined_at
) values (
    'slot-participant-001',
    'slot-room-001',
    'slot-user-001',
    'HOST',
    '2026-09-15T00:00:00Z'
);
