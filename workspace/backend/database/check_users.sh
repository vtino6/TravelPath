#!/bin/bash

# Script to check users in the database

DB_NAME="travelpath_db"
DB_USER="travelpath_user"
DB_PASSWORD="travelpath_password"

echo "=========================================="
echo "Users in the database:"
echo "=========================================="

PGPASSWORD=$DB_PASSWORD psql -h localhost -U $DB_USER -d $DB_NAME -c "
SELECT 
    id,
    name,
    email,
    CASE 
        WHEN password IS NULL THEN 'NULL (no password)'
        WHEN password = '' THEN 'EMPTY'
        ELSE 'HAS PASSWORD (' || LENGTH(password) || ' chars)'
    END as password_status,
    created_at,
    updated_at
FROM users
ORDER BY created_at DESC;
"

echo ""
echo "=========================================="
echo "Total number of users:"
echo "=========================================="

PGPASSWORD=$DB_PASSWORD psql -h localhost -U $DB_USER -d $DB_NAME -c "
SELECT COUNT(*) as total_users FROM users;
"

echo ""
echo "=========================================="
echo "Users with passwords:"
echo "=========================================="

PGPASSWORD=$DB_PASSWORD psql -h localhost -U $DB_USER -d $DB_NAME -c "
SELECT COUNT(*) as users_with_passwords 
FROM users 
WHERE password IS NOT NULL AND password != '';
"

echo ""
echo "=========================================="
echo "Users without passwords:"
echo "=========================================="

PGPASSWORD=$DB_PASSWORD psql -h localhost -U $DB_USER -d $DB_NAME -c "
SELECT COUNT(*) as users_without_passwords 
FROM users 
WHERE password IS NULL OR password = '';
"





