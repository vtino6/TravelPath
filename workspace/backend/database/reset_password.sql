-- Script to reset password for a user
-- Usage: psql -U travelpath_user -d travelpath_db -f reset_password.sql

-- First, let's see the current user
SELECT email, password FROM users WHERE email = 'valentino@gmail.com';

-- To reset the password, you need to generate a BCrypt hash
-- You can use an online tool like https://bcrypt-generator.com/
-- Or use this Python one-liner:
-- python3 -c "import bcrypt; print(bcrypt.hashpw(b'password123', bcrypt.gensalt()).decode())"

-- Example: If your password is "password123", the hash would be something like:
-- $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

-- Uncomment and update the hash below with your generated hash:
-- UPDATE users SET password = '$2a$10$YOUR_GENERATED_HASH_HERE' WHERE email = 'valentino@gmail.com';




