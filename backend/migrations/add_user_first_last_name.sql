-- Uruchom ręcznie na istniejącej bazie Postgres (create_all nie doda kolumn do już utworzonej tabeli).
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(120) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(120) NOT NULL DEFAULT '';
