/**
 * tunnelService.js
 * Manages public tunnel — either Cloudflare Quick Tunnel or ngrok.
 * Runs inside Termux, provides a public HTTPS URL for hosted projects.
 */

const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');
const PROJECTS_DIR = process.env.PROJECTS_DIR || `${process.env.HOME}/hostpanel-projects`;

let tunnelProcess = null;
let activeTunnelUrl = null;
let tunnelProvider = null; // 'cloudflare' | 'ngrok'
let tunnelTargetPort = null;
const tunnelLogFile = path.join(PROJECTS_DIR, 'tunnel.log');

// Listeners waiting for tunnel URL
const urlListeners = new Set();

function notifyUrl(url) {
  activeTunnelUrl = url;
  for (const cb of urlListeners) cb(url);
  urlListeners.clear();
}

// ─── Cloudflare Quick Tunnel ──────────────────────────────────────────────────
function startCloudflare(port) {
  const log = fs.createWriteStream(tunnelLogFile, { flags: 'a' });
  log.write(`\n[${new Date().toISOString()}] Starting Cloudflare tunnel → port ${port}\n`);

  const proc = spawn('cloudflared', ['tunnel', '--url', `http://localhost:${port}`], {
    stdio: ['ignore', 'pipe', 'pipe']
  });

  tunnelProcess = proc;
  tunnelProvider = 'cloudflare';
  tunnelTargetPort = port;

  // Cloudflare prints URL to stderr: "https://something.trycloudflare.com"
  const handleOutput = (data) => {
    const text = data.toString();
    log.write(text);
    const match = text.match(/https:\/\/[a-z0-9-]+\.trycloudflare\.com/);
    if (match && !activeTunnelUrl) {
      notifyUrl(match[0]);
      console.log(`🌐 Cloudflare Tunnel: ${match[0]}`);
    }
  };
  proc.stdout.on('data', handleOutput);
  proc.stderr.on('data', handleOutput);

  proc.on('exit', (code) => {
    log.write(`\n[exit: ${code}]\n`);
    tunnelProcess = null;
    activeTunnelUrl = null;
  });

  return proc;
}

// ─── ngrok Tunnel ─────────────────────────────────────────────────────────────
function startNgrok(port) {
  const log = fs.createWriteStream(tunnelLogFile, { flags: 'a' });
  log.write(`\n[${new Date().toISOString()}] Starting ngrok tunnel → port ${port}\n`);

  const proc = spawn('ngrok', ['http', `${port}`, '--log=stdout', '--log-format=json'], {
    stdio: ['ignore', 'pipe', 'pipe']
  });

  tunnelProcess = proc;
  tunnelProvider = 'ngrok';
  tunnelTargetPort = port;

  const handleOutput = (data) => {
    const text = data.toString();
    log.write(text);
    // ngrok logs JSON: {"url":"https://...ngrok-free.app",...}
    try {
      for (const line of text.split('\n')) {
        if (!line.trim()) continue;
        const obj = JSON.parse(line);
        if (obj.url && obj.url.startsWith('https://') && !activeTunnelUrl) {
          notifyUrl(obj.url);
          console.log(`🌐 ngrok Tunnel: ${obj.url}`);
        }
      }
    } catch {}
    // Fallback: plain URL match
    const match = text.match(/https:\/\/[a-z0-9-]+\.ngrok[a-z.-]*\.app/);
    if (match && !activeTunnelUrl) {
      notifyUrl(match[0]);
    }
  };
  proc.stdout.on('data', handleOutput);
  proc.stderr.on('data', handleOutput);

  proc.on('exit', (code) => {
    log.write(`\n[exit: ${code}]\n`);
    tunnelProcess = null;
    activeTunnelUrl = null;
  });

  return proc;
}

// ─── Public API ───────────────────────────────────────────────────────────────

async function startTunnel(port = 3001, provider = 'cloudflare') {
  if (tunnelProcess) {
    stopTunnel();
    await new Promise(r => setTimeout(r, 500));
  }
  activeTunnelUrl = null;

  if (provider === 'ngrok') {
    startNgrok(port);
  } else {
    startCloudflare(port);
  }

  // Wait up to 30s for URL to appear
  return new Promise((resolve) => {
    const timeout = setTimeout(() => resolve(null), 30000);
    urlListeners.add((url) => { clearTimeout(timeout); resolve(url); });
    if (activeTunnelUrl) { clearTimeout(timeout); resolve(activeTunnelUrl); }
  });
}

function stopTunnel() {
  if (tunnelProcess) {
    try { tunnelProcess.kill('SIGTERM'); } catch {}
    tunnelProcess = null;
  }
  activeTunnelUrl = null;
  tunnelProvider = null;
}

function getTunnelStatus() {
  return {
    running: !!tunnelProcess && !!activeTunnelUrl,
    url: activeTunnelUrl,
    provider: tunnelProvider,
    targetPort: tunnelTargetPort
  };
}

function isRunning() {
  return !!tunnelProcess;
}

module.exports = { startTunnel, stopTunnel, getTunnelStatus, isRunning };
