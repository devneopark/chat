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
