create table if not exists authentication_grant
(
    id                 varchar(32)              not null,
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

create table if not exists authentication_grant_20260817
    partition of authentication_grant
    for values from ('2026-08-17 00:00:00+00')
                 to ('2026-08-24 00:00:00+00');

create table if not exists authentication_grant_20260831
    partition of authentication_grant
    for values from ('2026-08-31 00:00:00+00')
                 to ('2026-09-07 00:00:00+00');

create unique index if not exists uk_authentication_grant_ac_jti
    on authentication_grant (ac_jti, rc_will_expires_at);
create unique index if not exists uk_authentication_grant_rc_id
    on authentication_grant (rc_id, rc_will_expires_at);
