-- V8: Fix incorrect password hash for admin@cerp.dev and the V7 dev personas.
--
-- V6's seeded hash for admin@cerp.dev never actually matched its documented password
-- ('Admin123!@#'), despite the comment claiming otherwise - login always returned
-- "invalid username or password" for every account using that hash. V7 copied the
-- same (broken) hash into its 20 seeded users for convenience, so it inherited the bug.
--
-- Replaces every row still carrying that broken hash with a freshly generated,
-- verified BCrypt hash (rounds=12) for the same password: 'Admin123!@#'.
-- john@cerp.dev / metal@cerp.dev are unaffected - they use a different, working hash
-- generated at runtime by SeedDataConfig for password 'secret'.

UPDATE identity.user_accounts
SET password_hash = '$2a$12$caYIiqtdkGDYSGP2Um7iB.lFCKQE52bZsf3tTwyV94J1kh.gnxoLm'
WHERE password_hash = '$2a$12$L1.pPtTOq3uDmGMbKDEatuxDR.oiSX5WMQLNXAKZYusKbWHEBk9dq';
