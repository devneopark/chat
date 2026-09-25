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

insert into room(
    id,
    title
) values
    ('slot-count-001', 'Slot Count Room'),
    ('slot-additional-001', 'Slot Additional Room'),
    ('slot-empty-001', 'Slot Empty Room'),
    ('slot-delete-001', 'Slot Delete Room');

insert into admission_slot(
    room_id,
    slot_number
) values
    ('slot-count-001', 1),
    ('slot-count-001', 2),
    ('slot-additional-001', 1),
    ('slot-additional-001', 3),
    ('slot-empty-001', 1),
    ('slot-empty-001', 2),
    ('slot-empty-001', 3),
    ('slot-delete-001', 1),
    ('slot-delete-001', 2),
    ('slot-delete-001', 3);
