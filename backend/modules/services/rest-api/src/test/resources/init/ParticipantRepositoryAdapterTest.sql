insert into users(
    id,
    principal,
    password_hash,
    display_name
) values (
    'participant-user-001',
    'participant.seed.principal',
    'seed-hashed-password',
    'Participant Seed User'
);

insert into room(
    id,
    title
) values (
    'participant-room-001',
    'Participant Seed Room'
);

insert into users(
    id,
    principal,
    password_hash,
    display_name
) values (
    'participant-user-002',
    'participant.second.principal',
    'seed-hashed-password',
    'Participant Second User'
);

insert into users(
    id,
    principal,
    password_hash,
    display_name
) values (
    'participant-user-003',
    'participant.third.principal',
    'seed-hashed-password',
    'Participant Third User'
);

insert into participant(
    id,
    room_id,
    user_id,
    participant_role,
    joined_at
) values (
    'participant-seed-001',
    'participant-room-001',
    'participant-user-001',
    'HOST',
    '2026-09-14T00:00:00Z'
);

insert into participant(
    id,
    room_id,
    user_id,
    participant_role,
    joined_at,
    exited_at
) values (
    'participant-exited-only-001',
    'participant-room-001',
    'participant-user-003',
    'GUEST',
    '2026-09-14T02:00:00Z',
    '2026-09-15T00:00:00Z'
);

insert into participant(
    id,
    room_id,
    user_id,
    participant_role,
    joined_at,
    exited_at
) values (
    'participant-exited-001',
    'participant-room-001',
    'participant-user-001',
    'GUEST',
    '2026-09-14T01:00:00Z',
    '2026-09-15T00:00:00Z'
);
