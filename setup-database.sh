#!/bin/bash

# Database setup script for FSA Capstone 2026
# This script will drop and recreate the database schema using Liquibase

set -e  # Exit on any error

echo "════════════════════════════════════════════════════════════════════"
echo "🗄️  FSA Capstone 2026 - Database Setup Script"
echo "════════════════════════════════════════════════════════════════════"

# Database configuration
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-speakvn_db}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-postgres}

# Export PostgreSQL password for non-interactive authentication
export PGPASSWORD=$DB_PASSWORD

echo ""
echo "📋 Database Configuration:"
echo "   Host: $DB_HOST"
echo "   Port: $DB_PORT"
echo "   Database: $DB_NAME"
echo "   User: $DB_USER"
echo ""

# Check if psql is installed
if ! command -v psql &> /dev/null; then
    echo "❌ Error: psql is not installed. Please install PostgreSQL client."
    exit 1
fi

# Check database connection
echo "🔗 Checking database connection..."
if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" &> /dev/null; then
    echo "✅ Database connection successful"
else
    echo "❌ Error: Cannot connect to database. Please check your connection settings."
    exit 1
fi

echo ""
echo "⚠️  WARNING: This will DROP all tables and recreate the schema!"
echo ""
read -p "Continue? (yes/no): " -r
echo
if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "Cancelled."
    exit 0
fi

echo ""
echo "🗑️  Dropping existing tables..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << EOF
DROP TABLE IF EXISTS account_badge CASCADE;
DROP TABLE IF EXISTS attempt_phoneme_feedback CASCADE;
DROP TABLE IF EXISTS attempt CASCADE;
DROP TABLE IF EXISTS practice_session CASCADE;
DROP TABLE IF EXISTS prediction CASCADE;
DROP TABLE IF EXISTS challenge CASCADE;
DROP TABLE IF EXISTS classroom_member CASCADE;
DROP TABLE IF EXISTS classroom CASCADE;
DROP TABLE IF EXISTS level CASCADE;
DROP TABLE IF EXISTS badge CASCADE;
DROP TABLE IF EXISTS dialect CASCADE;
DROP TABLE IF EXISTS language CASCADE;
DROP TABLE IF EXISTS country CASCADE;
DROP TABLE IF EXISTS user_profile CASCADE;
DROP TABLE IF EXISTS account CASCADE;
DROP TABLE IF EXISTS databasechangelog;
DROP TABLE IF EXISTS databasechangeloglock;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

SELECT 'All tables dropped successfully' AS status;
EOF

echo "✅ Tables dropped successfully"

echo ""
echo "🚀 Rebuilding application..."
cd "$(dirname "$0")"
mvn clean compile -q

echo ""
echo "🔄 Running Liquibase migration..."
mvn liquibase:update -q

echo ""
echo "════════════════════════════════════════════════════════════════════"
echo "✅ Database setup completed successfully!"
echo "════════════════════════════════════════════════════════════════════"
echo ""
echo "📝 Next steps:"
echo "   1. Start the application: mvn spring-boot:run"
echo "   2. Access API documentation: http://localhost:8082/swagger-ui.html"
echo ""

