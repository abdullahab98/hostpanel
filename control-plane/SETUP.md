# HostPanel Control Plane — Setup Guide

## Prerequisites
- Linux server (Ubuntu 22.04 recommended)
- Docker Engine installed
- Domain name (optional, but needed for HTTPS)
- Node.js 20+ (for running without Docker Compose)

---

## Step 1: Deploy the Control Plane

```bash
# Clone or copy the control-plane folder to your server
scp -r control-plane/ user@yourserver.com:/opt/hostpanel/

# SSH into your server
ssh user@yourserver.com

cd /opt/hostpanel/control-plane

# Create your .env file
cp .env.example .env
nano .env
```

Edit `.env`:
```env
PORT=3001
JWT_SECRET=your-very-long-random-secret-key-here-minimum-32-chars
CADDY_ADMIN_URL=http://localhost:2019
DOCKER_SOCKET=/var/run/docker.sock
PROJECTS_DIR=/opt/hostpanel/projects
SERVER_DOMAIN=yourserver.com       # Optional
CLOUDFLARE_TUNNEL_TOKEN=           # Optional
```

---

## Step 2: Start the Stack

```bash
# One command to start everything
docker compose up -d

# Check logs
docker compose logs -f
```

This starts:
- **hostpanel-api** on port `3001` (Control Plane API)
- **hostpanel-caddy** on ports `80`, `443`, `2019` (Reverse Proxy + SSL)

---

## Step 3: Configure the Android App

1. Open **HostPanel** app
2. Go to **Settings** (bottom nav)
3. Enter:
   - **Server URL**: `https://yourserver.com:3001` (or your server IP if no domain)
   - **API Secret Key**: same value as `JWT_SECRET` in your `.env`
4. Tap **Test Connection** — should show ✅ Connected
5. Tap **Save**

---

## Step 4: Deploy Your First Project

1. In the app, tap **Deploy Project** (FAB)
2. **Step 1**: Enter your Git repository URL
3. **Step 2**: Select framework (Node.js, Next.js, React, Django, Spring Boot, or Static)
4. **Step 3**: Set project name and environment variables
5. **Step 4**: Choose auto-subdomain or custom domain
6. **Step 5**: Watch the live build logs!

---

## Step 5: Custom Domains & SSL

### Option A: Subdomain (easiest)
Set `SERVER_DOMAIN=yourserver.com` in `.env`  
Projects automatically get `projectname.yourserver.com` with free SSL.

### Option B: Custom Domain
1. Go to **Domains** tab in app
2. Tap **+** to add domain
3. Enter `myapp.com` and project name
4. Point your domain's A record to your server's IP:
   ```
   A  myapp.com  →  YOUR_SERVER_IP
   ```
5. Tap **Verify DNS** — Caddy auto-issues Let's Encrypt SSL within 60 seconds!

---

## Step 6: Public Access Without a VPS (Cloudflare Tunnel)

If you're running on a home server or don't have a static IP:

1. Sign up at [Cloudflare Zero Trust](https://one.cloudflare.com) (free)
2. Create a tunnel → copy the **Tunnel Token**
3. In HostPanel app → Settings → paste the token → Enable Tunnel
4. In Domains tab → toggle **Tunnel switch** to ON

Your server is now accessible from anywhere via Cloudflare's global network!

---

## API Reference

All endpoints require: `Authorization: Bearer YOUR_JWT_SECRET`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check (no auth) |
| GET | `/api/metrics` | CPU, RAM, disk, uptime |
| GET | `/api/containers` | List all projects |
| POST | `/api/deploy` | Deploy new project |
| POST | `/api/containers/:name/start` | Start project |
| POST | `/api/containers/:name/stop` | Stop project |
| POST | `/api/containers/:name/restart` | Restart project |
| POST | `/api/containers/:name/rebuild` | Rebuild image + restart |
| DELETE | `/api/containers/:name` | Delete project |
| GET | `/api/containers/:name/logs` | Last 200 log lines |
| WS | `/api/containers/:name/logs/ws` | Live log stream |
| GET | `/api/domains` | List custom domains |
| POST | `/api/domains` | Add custom domain |
| DELETE | `/api/domains/:domain` | Remove domain |
| POST | `/api/domains/:domain/verify` | Check DNS propagation |
| POST | `/api/domains/:domain/ssl/renew` | Force SSL renewal |
| GET | `/api/audit-logs` | Security audit trail |
| POST | `/api/tunnel/start` | Start Cloudflare Tunnel |
| POST | `/api/tunnel/stop` | Stop Cloudflare Tunnel |

---

## Supported Frameworks

| Framework | Detection | Port | Notes |
|-----------|-----------|------|-------|
| **Node.js** | `package.json` | 3000 | Express, Fastify, etc. |
| **Next.js** | next in deps | 3000 | Uses standalone output |
| **React** | react-scripts/vite | 80 | Served via nginx |
| **Django** | `manage.py` | 8000 | Requires `requirements.txt` |
| **Spring Boot** | `pom.xml` / `build.gradle` | 8080 | Maven or Gradle |
| **Static HTML** | `index.html` | 80 | Pure HTML/CSS/JS via nginx |

---

## Firewall Setup

```bash
# Allow required ports
ufw allow 22/tcp      # SSH
ufw allow 80/tcp      # HTTP (redirect to HTTPS)
ufw allow 443/tcp     # HTTPS
ufw allow 3001/tcp    # Control Plane API (optional: remove after Caddy setup)
ufw enable
```

---

## Troubleshooting

**"Cannot reach server"**  
→ Check firewall: `ufw status`  
→ Check if API is running: `docker compose ps`  
→ Try: `curl http://yourserver.com:3001/api/health`

**SSL not issuing**  
→ Ensure your domain's A record points to server IP  
→ Wait 2-3 minutes for Let's Encrypt propagation  
→ Check Caddy logs: `docker compose logs caddy`

**Build fails**  
→ Check deployment logs in app's Project Detail screen  
→ Ensure your repo's framework files are in the root directory  
→ For Next.js: set `output: 'standalone'` in `next.config.js`
