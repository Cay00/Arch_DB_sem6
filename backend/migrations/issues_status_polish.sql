-- Opcjonalnie ręcznie na istniejącej bazie (init_db też robi UPDATE przy starcie).
UPDATE issues SET status = 'Zgłoszone' WHERE status IN ('NEW', 'new');
