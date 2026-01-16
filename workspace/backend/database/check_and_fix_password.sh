#!/bin/bash

# Script to check and add password column if missing

DB_NAME="travelpath_db"
DB_USER="travelpath_user"
DB_PASSWORD="travelpath_password"

echo "Checking users table structure..."

# Check if password column exists
PGPASSWORD=$DB_PASSWORD psql -h localhost -U $DB_USER -d $DB_NAME -c "
SELECT column_name, data_type, is_nullable
FROM information_schema.columns 
WHERE table_name = 'users' 
ORDER BY ordinal_position;
"

echo ""
echo "Checking if password column exists..."
HAS_PASSWORD=$(PGPASSWORD=$DB_PASSWORD psql -h localhost -U $DB_USER -d $DB_NAME -t -c "
SELECT COUNT(*) 
FROM information_schema.columns 
WHERE table_name = 'users' AND column_name = 'password';
")

if [ "$HAS_PASSWORD" -eq 0 ]; then
    echo "Password column does NOT exist. Adding it..."
    PGPASSWORD=$DB_PASSWORD psql -h localhost -U $DB_USER -d $DB_NAME -c "
    ALTER TABLE users ADD COLUMN password VARCHAR(255);
    "
    echo "Password column added successfully!"
else
    echo "Password column already exists."
fi

echo ""
echo "Final table structure:"
PGPASSWORD=$DB_PASSWORD psql -h localhost -U $DB_USER -d $DB_NAME -c "
SELECT column_name, data_type, is_nullable
FROM information_schema.columns 
WHERE table_name = 'users' 
ORDER BY ordinal_position;
"





