-- Postgres: stare tabele users (password_hash, role) → nowy model API.
-- Przy starcie backendu init_db wykonuje to samo automatycznie.

ALTER TABLE users ADD COLUMN IF NOT EXISTS firebase_uid VARCHAR(128);
CREATE UNIQUE INDEX IF NOT EXISTS ix_users_firebase_uid ON users (firebase_uid);

ALTER TABLE users ADD COLUMN IF NOT EXISTS hashed_password VARCHAR(255);
UPDATE users SET hashed_password = password_hash
WHERE hashed_password IS NULL AND password_hash IS NOT NULL;

ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(255) NOT NULL DEFAULT '';
UPDATE users SET display_name = trim(first_name || ' ' || last_name)
WHERE (display_name IS NULL OR display_name = '')
  AND trim(coalesce(first_name, '') || ' ' || coalesce(last_name, '')) <> '';
UPDATE users SET display_name = split_part(email, '@', 1)
WHERE display_name IS NULL OR display_name = '';

ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();

UPDATE users SET account_type = role
WHERE role IS NOT NULL AND trim(role) <> ''
  AND (account_type IS NULL OR account_type = 'citizen');

UPDATE users SET account_type = lower(trim(account_type)) WHERE account_type IS NOT NULL;
UPDATE users SET account_type = 'citizen'
WHERE account_type IS NULL OR account_type = '' OR account_type NOT IN ('citizen', 'official');

ALTER TABLE users DROP COLUMN IF EXISTS password_hash;
ALTER TABLE users DROP COLUMN IF EXISTS role;
