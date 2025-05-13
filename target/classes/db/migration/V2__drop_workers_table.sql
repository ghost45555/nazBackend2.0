-- Drop foreign key constraints first if they exist
ALTER TABLE IF EXISTS worker_roles DROP CONSTRAINT IF EXISTS fk_worker_roles_worker;
ALTER TABLE IF EXISTS worker_roles DROP CONSTRAINT IF EXISTS fk_worker_roles_role;

-- Drop the worker_roles junction table if it exists
DROP TABLE IF EXISTS worker_roles;

-- Drop the workers table if it exists
DROP TABLE IF EXISTS workers; 