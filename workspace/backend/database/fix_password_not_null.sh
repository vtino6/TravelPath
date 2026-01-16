#!/bin/bash

# Script to make password column NOT NULL (after ensuring all users have passwords)

DB_NAME="travelpath_db"
DB_USER="travelpath_user"
DB_PASSWORD="travelpath_password"

echo "Checking for existing users without passwords..."

# Check if there are any users without passwords
USERS_WITHOUT_PASSWORD=$(PGPASSWORD=$DB_PASSWORD psql -h localhost -U $DB_USER -d $DB_NAME -t -c "
SELECT COUNT(*) 
FROM users 
WHERE password IS NULL;
")

echo "Found $USERS_WITHOUT_PASSWORD users without passwords"

if [ "$USERS_WITHOUT_PASSWORD" -gt 0 ]; then
    echo ""
    echo "WARNING: There are users without passwords."
    echo "Options:"
    echo "1. Delete all existing users (recommended for development)"
    echo "2. Keep them and leave password column nullable"
    echo ""
    read -p "Do you want to delete all existing users? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Deleting all existing users..."
        PGPASSWORD=$DB_PASSWORD psql -h localhost -U $DB_USER -d $DB_NAME -c "DELETE FROM users;"
        echo "All users deleted."
    else
        echo "Keeping existing users. Password column will remain nullable."
        exit 0
    fi
fi

echo ""
echo "Making password column NOT NULL..."
PGPASSWORD=$DB_PASSWORD psql -h localhost -U $DB_USER -d $DB_NAME -c "
ALTER TABLE users ALTER COLUMN password SET NOT NULL;
"

echo ""
echo "Final table structure:"
PGPASSWORD=$DB_PASSWORD psql -h localhost -U $DB_USER -d $DB_NAME -c "
SELECT column_name, data_type, is_nullable
FROM information_schema.columns 
WHERE table_name = 'users' 
ORDER BY ordinal_position;
"

echo ""
echo "Done! Password column is now NOT NULL."





