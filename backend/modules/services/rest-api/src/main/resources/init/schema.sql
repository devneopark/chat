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
    id                 varchar(32) primary key,
    user_id            varchar(32)              not null,
    issued_at          timestamp with time zone not null default current_timestamp,
    ac_jti             varchar(50)              not null,
    ac_issued_at       timestamp with time zone not null,
    ac_will_expires_at timestamp with time zone not null,
    rc_id              varchar(255)             not null,
    rc_issued_at       timestamp with time zone not null,
    rc_will_expires_at timestamp with time zone not null
);

create unique index if not exists uk_authentication_grant_ac_jti on authentication_grant (ac_jti);
create unique index if not exists uk_authentication_grant_rc_id on authentication_grant (rc_id);
