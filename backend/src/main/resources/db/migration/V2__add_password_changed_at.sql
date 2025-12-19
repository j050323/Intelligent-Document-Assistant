-- Add password_changed_at column to users table
-- This column tracks when the user's password was last changed
-- Used to invalidate JWT tokens issued before password change

ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP;

-- Add comment to explain the column's purpose
COMMENT ON COLUMN users.password_changed_at IS 'Timestamp of last password change, used to invalidate old JWT tokens';
