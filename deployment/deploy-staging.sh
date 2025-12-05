#!/bin/bash

# Manual Deployment Script for Staging Environment
# Run this on deti-tqs-13.ua.pt VM

set -e

DEPLOY_DIR=~/gamerent-staging
BRANCH=${1:-develop}

echo "Deploying GameRent Staging Environment..."
echo "Branch: $BRANCH"

cd $DEPLOY_DIR

# Pull latest changes
echo "Pulling latest code..."
git fetch origin
git checkout $BRANCH
git pull origin $BRANCH

# Stop existing containers
echo "Stopping existing containers..."
docker compose -f docker-compose.staging.yml down

# Build and start services
echo "Building and starting services..."
docker compose -f docker-compose.staging.yml up -d --build

# Wait for services to be healthy
echo "Waiting for services to start..."
sleep 30

# Check service status
echo "Service Status:"
docker compose -f docker-compose.staging.yml ps

# Health check
echo "Running health check..."
if curl -f http://localhost:8081/actuator/health 2>/dev/null; then
    echo "Staging deployment successful!"
    echo "Backend: http://deti-tqs-13.ua.pt:8081"
    echo "Frontend: http://deti-tqs-13.ua.pt:3001"
else
    echo "Health check failed!"
    echo "Checking logs..."
    docker compose -f docker-compose.staging.yml logs --tail=50
    exit 1
fi
