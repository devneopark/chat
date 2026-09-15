create table if not exists users
(
    id            varchar(32) primary key,
    principal     varchar(50)              not null,
    password_hash text                     not null,
    display_name  varchar(50)              not null,
    registered_at timestamp with time zone not null default current_timestamp,
    withdrawn_at  timestamp with time zone
);

create unique index if not exists uk_users_active_principal
    on users (principal)
    where withdrawn_at is null;

create table if not exists authentication_grant
(
    id                 varchar(32)             not null,
    user_id            varchar(32)              not null,
    issued_at          timestamp with time zone not null default current_timestamp,
    ac_jti             varchar(50)              not null,
    ac_issued_at       timestamp with time zone not null,
    ac_will_expires_at timestamp with time zone not null,
    rc_id              varchar(255)             not null,
    rc_issued_at       timestamp with time zone not null,
    rc_will_expires_at timestamp with time zone not null,
    primary key (id, rc_will_expires_at)
) partition by range (rc_will_expires_at);

create unique index if not exists uk_authentication_grant_ac_jti
    on authentication_grant (ac_jti, rc_will_expires_at);
create unique index if not exists uk_authentication_grant_rc_id
    on authentication_grant (rc_id, rc_will_expires_at);

create table if not exists room
(
    id            varchar(32) primary key,
    title         varchar(50) not null,
    password_hash text,
    closed_at     timestamp with time zone,
    opened_at     timestamp with time zone not null default current_timestamp
);

create unique index if not exists uk_room_title
    on room (title)
    where closed_at is null;

create table if not exists participant
(
    id               varchar(32) primary key,
    room_id          varchar(32)              not null references room (id),
    user_id          varchar(32)              not null references users (id),
    participant_role varchar(10)              not null,
    joined_at        timestamp with time zone not null default current_timestamp,
    exited_at        timestamp with time zone
);

create unique index if not exists uk_participant_room_user
    on participant (room_id, user_id)
    where exited_at is null;

create table if not exists admission_slot
(
    room_id                 varchar(32) not null references room (id),
    slot_number             integer     not null,
    occupant_participant_id varchar(32) references participant (id),
    primary key (room_id, slot_number)
);
