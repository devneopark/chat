insert into authentication_grant (
  id,
  user_id,
  issued_at,
  ac_jti,
  ac_issued_at,
  ac_will_expires_at,
  rc_id,
  rc_issued_at,
  rc_will_expires_at
) values (
  'seed-grant-001',
  'seed-user-001',
  timestamp with time zone '2026-08-11 00:00:00+00',
  'seed-access-jti-001',
  timestamp with time zone '2026-08-11 00:00:01+00',
  timestamp with time zone '2026-08-11 00:15:01+00',
  'seed-renewal-id-001',
  timestamp with time zone '2026-08-11 00:00:02+00',
  timestamp with time zone '2026-08-18 00:00:02+00'
);
