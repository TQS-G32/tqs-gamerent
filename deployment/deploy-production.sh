#!/bin/bash

# Manual Deployment Script for Production Environment
# Run this on deti-tqs-13.ua.pt VM

set -e

DEPLOY_DIR=~/gamerent-production

echo "Deploying GameRent Production Environment..."
echo "This will deploy from the main branch"

read -p "Are you sure you want to deploy to production? (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo "Deployment cancelled"
    exit 1
fi

cd $DEPLOY_DIR

# Pull latest changes from main
echo "Pulling latest code from main..."
git fetch origin
git checkout main
git pull origin main

# Stop existing containers
echo "Stopping existing containers..."
docker compose -f docker-compose.prod.yml down

# Backup database (optional but recommended)
echo "Creating database backup..."
BACKUP_DIR=~/backups
mkdir -p $BACKUP_DIR
BACKUP_FILE="$BACKUP_DIR/gamerent-db-$(date +%Y%m%d-%H%M%S).sql"
docker compose -f docker-compose.prod.yml up -d db
sleep 10
docker exec gamerent-prod-db pg_dump -U admin gamerent_db > $BACKUP_FILE 2>/dev/null || true
docker compose -f docker-compose.prod.yml down

echo "Backup saved to: $BACKUP_FILE"

# Build and start services
echo "Building and starting services..."
docker compose -f docker-compose.prod.yml up -d --build

# Wait for services to be healthy
echo "Waiting for services to start..."
sleep 40

# Check service status
echo "Service Status:"
docker compose -f docker-compose.prod.yml ps

# Health check
echo "Running health check..."
if curl -f http://localhost:8080/actuator/health 2>/dev/null; then
    echo "Production deployment successful!"
    echo "Backend: http://deti-tqs-13.ua.pt:8080"
    echo "Frontend: http://deti-tqs-13.ua.pt"
else
    echo "Health check failed!"
    echo "Checking logs..."
    docker compose -f docker-compose.prod.yml logs --tail=50
    exit 1
fi

# Clean up old Docker images
echo "Cleaning up old Docker images..."
docker image prune -f
