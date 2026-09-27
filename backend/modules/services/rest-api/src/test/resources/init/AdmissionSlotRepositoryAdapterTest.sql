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

insert into users(
    id,
    principal,
    password_hash,
    display_name
) values (
    'slot-user-002',
    'slot.second.seed.principal',
    'seed-hashed-password',
    'Slot Second Seed User'
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
    ('slot-assign-001', 'Slot Assign Room'),
    ('slot-no-empty-001', 'Slot No Empty Room');

insert into participant(
    id,
    room_id,
    user_id,
    participant_role,
    joined_at
) values
    ('slot-participant-003', 'slot-assign-001', 'slot-user-002', 'HOST', '2026-09-15T00:00:00Z'),
    ('slot-participant-004', 'slot-no-empty-001', 'slot-user-002', 'HOST', '2026-09-15T00:00:00Z');

insert into room(
    id,
    title
) values (
    'slot-join-001',
    'Slot Join Room'
);

insert into participant(
    id,
    room_id,
    user_id,
    participant_role,
    joined_at
) values (
    'slot-participant-002',
    'slot-join-001',
    'slot-user-002',
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

insert into admission_slot(
    room_id,
    slot_number,
    occupant_participant_id
) values
    ('slot-join-001', 1, 'slot-participant-002'),
    ('slot-join-001', 2, null),
    ('slot-join-001', 3, null),
    ('slot-assign-001', 1, null),
    ('slot-no-empty-001', 1, 'slot-participant-004');
