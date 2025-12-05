#!/bin/bash

# VM Setup Script for GameRent Application
# Run this script on deti-tqs-13.ua.pt VM as user

set -e

echo "Setting up GameRent deployment environment..."

# Update system
echo "Updating system packages..."
sudo apt update
sudo apt install -y git docker.io docker-compose-plugin curl

# Enable and start Docker
echo "Setting up Docker..."
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker $USER

# Create directories for staging and production
echo "Creating deployment directories..."
mkdir -p ~/gamerent-staging
mkdir -p ~/gamerent-production

# Clone repository for staging
echo "Setting up staging environment..."
if [ ! -d ~/gamerent-staging/.git ]; then
    cd ~/gamerent-staging
    git clone https://github.com/TQS-G32/tqs-gamerent.git .
else
    cd ~/gamerent-staging
    git pull
fi

# Clone repository for production
echo "Setting up production environment..."
if [ ! -d ~/gamerent-production/.git ]; then
    cd ~/gamerent-production
    git clone https://github.com/TQS-G32/tqs-gamerent.git .
    git checkout main
else
    cd ~/gamerent-production
    git pull
fi

# Create environment files (templates)
echo "Creating environment file templates..."

cat > ~/gamerent-staging/.env.staging << 'EOF'
# Staging Environment Variables
IGDB_CLIENT_ID=your_client_id_here
IGDB_AUTH_TOKEN=your_auth_token_here
POSTGRES_USER=admin
POSTGRES_PASSWORD=staging_secret_password
EOF

cat > ~/gamerent-production/.env.production << 'EOF'
# Production Environment Variables
IGDB_CLIENT_ID=your_client_id_here
IGDB_AUTH_TOKEN=your_auth_token_here
POSTGRES_USER=admin
POSTGRES_PASSWORD=production_secret_password
EOF

echo "Setup complete!"
echo ""
echo "IMPORTANT: Edit the following files with your actual credentials:"
echo "   - ~/gamerent-staging/.env.staging"
echo "   - ~/gamerent-production/.env.production"
echo ""
echo "Next steps:"
echo "   1. Edit the .env files with your credentials"
echo "   2. Log out and log back in for Docker group changes to take effect"
echo "   3. Test with: docker ps"
