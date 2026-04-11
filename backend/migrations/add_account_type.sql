-- Postgres: typ konta (citizen | official). Przy starcie backendu init_db też doda kolumnę na SQLite.
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_type VARCHAR(20) NOT NULL DEFAULT 'citizen';
