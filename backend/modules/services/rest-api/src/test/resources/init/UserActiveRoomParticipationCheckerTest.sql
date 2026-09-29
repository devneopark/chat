insert into users(
    id,
    principal,
    password_hash,
    display_name
) values
    ('active-room-user-001', 'active.room.user.001', 'hashed-password', 'Active Room User'),
    ('active-room-user-002', 'active.room.user.002', 'hashed-password', 'Exited Room User'),
    ('active-room-user-003', 'active.room.user.003', 'hashed-password', 'Empty Room User'),
    ('active-room-user-004', 'active.room.user.004', 'hashed-password', 'Active And Exited Room User');

insert into room(
    id,
    title
) values (
    'active-room-check-001',
    'Active Room Check Room'
);

insert into participant(
    id,
    room_id,
    user_id,
    participant_role,
    joined_at,
    exited_at
) values
    ('active-room-participant-001', 'active-room-check-001', 'active-room-user-001', 'GUEST', '2026-09-28T00:00:00Z', null),
    ('active-room-participant-002', 'active-room-check-001', 'active-room-user-002', 'GUEST', '2026-09-28T00:00:00Z', '2026-09-28T01:00:00Z'),
    ('active-room-participant-003', 'active-room-check-001', 'active-room-user-004', 'GUEST', '2026-09-28T00:00:00Z', '2026-09-28T01:00:00Z'),
    ('active-room-participant-004', 'active-room-check-001', 'active-room-user-004', 'GUEST', '2026-09-28T00:00:00Z', null);
