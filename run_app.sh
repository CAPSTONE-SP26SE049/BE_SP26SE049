#!/bin/bash
# =========================================================================
# Smart Port Manager - Auto-clean port 8082 before starting application
# =========================================================================

echo "╔══════════════════════════════════════════════════════════════════════════════════════╗"
echo "║                    🚀 SMART PORT MANAGER - Spring Boot Starter                       ║"
echo "╚══════════════════════════════════════════════════════════════════════════════════════╝"
echo ""

PORT=8082
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Function to kill process on port
kill_port() {
    local port=$1
    echo "🔍 Checking port $port..."

    # Get PID of process using the port
    PID=$(lsof -i :$port 2>/dev/null | awk 'NR!=1 {print $2}' | head -1)

    if [ -z "$PID" ]; then
        echo -e "${GREEN}✓${NC} Port $port is free"
        return 0
    fi

    echo -e "${YELLOW}⚠${NC}  Process $PID is using port $port"
    echo "   Killing process..."

    # Try graceful kill first
    kill -15 $PID 2>/dev/null
    sleep 1

    # Force kill if still running
    if kill -0 $PID 2>/dev/null; then
        kill -9 $PID 2>/dev/null
        echo -e "${RED}✓${NC} Force killed process $PID"
    else
        echo -e "${GREEN}✓${NC} Gracefully stopped process $PID"
    fi

    sleep 1
    return 0
}

# Function to compile project
compile_project() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📦 Compiling project..."
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    mvn clean compile -DskipTests -q 2>&1 | tail -5

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓${NC} Compilation successful"
    else
        echo -e "${RED}❌ Compilation failed${NC}"
        exit 1
    fi
}

# Function to start application
start_application() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "🚀 Starting Spring Boot Application..."
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "📍 Server running on:"
    echo "   http://localhost:$PORT"
    echo ""
    echo "📚 API Documentation:"
    echo "   http://localhost:$PORT/swagger-ui.html"
    echo ""
    echo "💾 Database: PostgreSQL (jdbc:postgresql://localhost:5432/speakvn_db)"
    echo ""
    echo "Press Ctrl+C to stop the server"
    echo ""

    mvn spring-boot:run
}

# Main execution
echo "🔄 Pre-flight checks..."
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven is not installed${NC}"
    exit 1
fi
echo -e "${GREEN}✓${NC} Maven is installed"

# Check if PostgreSQL is running
if ! pg_isready -h localhost -U postgres > /dev/null 2>&1; then
    echo -e "${RED}❌ PostgreSQL is not running${NC}"
    echo "   Start PostgreSQL and try again"
    exit 1
fi
echo -e "${GREEN}✓${NC} PostgreSQL is running"

# Kill any process using port 8082
echo ""
kill_port $PORT

# Compile and start
compile_project
start_application

