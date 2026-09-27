insert into room(
    id,
    title,
    password_hash
) values (
    'room-seed-001',
    'Seed Room',
    'seed-hashed-password'
);

insert into room(
    id,
    title,
    password_hash
) values (
    'room-update-001',
    'Room To Update',
    'update-seed-password'
);

insert into room(
    id,
    title
) values (
    'room-close-001',
    'Room To Close'
);

insert into room(
    id,
    title,
    closed_at
) values (
    'room-closed-001',
    'Seed Room',
    '2026-09-15T00:00:00Z'
);

insert into room(
    id,
    title,
    closed_at
) values (
    'room-closed-only-001',
    'Closed Room',
    '2026-09-15T00:00:00Z'
);
