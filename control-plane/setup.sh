#!/data/data/com.termux/files/usr/bin/bash

# 🚀 HostPanel Android-Native Setup Script (Termux)
# This script prepares Termux for HostPanel and starts the control plane.

echo "========================================"
echo "    HostPanel Control Plane Setup       "
echo "========================================"

# Request storage permission if not already granted
echo "1. Requesting storage permission..."
termux-setup-storage
sleep 2

# Update packages
echo "2. Updating package lists..."
pkg update -y

# Install essential dependencies
echo "3. Installing native dependencies..."
pkg install -y nodejs python git openjdk-17 mariadb postgresql redis cloudflared openssh

# Install global node tools
echo "4. Installing PM2 and Serve..."
npm install -g pm2 serve

# Prepare HostPanel directories
echo "5. Preparing directories..."
mkdir -p ~/hostpanel-projects
mkdir -p ~/hostpanel-dbs
mkdir -p ~/hostpanel-control-plane

# Clone or update the control plane (mock step - assuming user puts the files in ~/hostpanel-control-plane)
# In production, we would pull from a GitHub repo:
# if [ ! -d ~/hostpanel-control-plane/.git ]; then
#   git clone https://github.com/hostpanel/hostpanel-control-plane.git ~/hostpanel-control-plane
# else
#   cd ~/hostpanel-control-plane && git pull
# fi

echo "6. Installing Control Plane Node.js dependencies..."
cd ~/hostpanel-control-plane
# For now we assume the files are already here or will be pushed here via the app.
if [ -f "package.json" ]; then
    npm install
fi

# Set up auto-start in bashrc
BASHRC=~/.bashrc
if ! grep -q "pm2 resurrect" "$BASHRC"; then
    echo "pm2 resurrect &> /dev/null" >> "$BASHRC"
fi

echo "========================================"
echo "✅ Setup Complete!"
echo "To start the Control Plane manually, run:"
echo "  cd ~/hostpanel-control-plane && npm start"
echo ""
echo "Open the HostPanel App to connect!"
echo "========================================"
