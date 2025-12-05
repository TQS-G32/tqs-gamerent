#!/bin/bash

# Rollback Script for Production
# Use this to rollback to a previous commit

set -e

DEPLOY_DIR=~/gamerent-production
COMMIT_HASH=${1}

if [ -z "$COMMIT_HASH" ]; then
    echo "Error: Please provide a commit hash to rollback to"
    echo "Usage: ./rollback.sh <commit-hash>"
    echo ""
    echo "Recent commits:"
    cd $DEPLOY_DIR
    git log --oneline -10
    exit 1
fi

echo "Rolling back to commit: $COMMIT_HASH"

read -p "Are you sure you want to rollback production? (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo "Rollback cancelled"
    exit 1
fi

cd $DEPLOY_DIR

# Stop containers
docker compose -f docker-compose.prod.yml down

# Rollback code
git checkout $COMMIT_HASH

# Rebuild and start
docker compose -f docker-compose.prod.yml up -d --build

echo "Waiting for services to start..."
sleep 40

# Health check
if curl -f http://localhost:8080/actuator/health 2>/dev/null; then
    echo "Rollback successful!"
else
    echo "Rollback failed - check logs"
    docker compose -f docker-compose.prod.yml logs --tail=50
    exit 1
fi
