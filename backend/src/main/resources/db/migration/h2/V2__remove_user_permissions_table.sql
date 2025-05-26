-- Remove unused user_permissions table and its index
-- This table was created in V1 with more complex features in mind,
-- but is not used anywhere in the application as that feature was dropped

-- Drop the index first (if it exists)
DROP INDEX IF EXISTS idx_user_permissions_user_id;

-- Drop the user_permissions table (if it exists)
DROP TABLE IF EXISTS user_permissions; 