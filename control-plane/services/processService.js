/**
 * processService.js
 * Android-native process manager — replaces Docker entirely.
 * Runs Node.js, Python, React, Next.js, PHP, Static projects
 * directly as child processes inside Termux's Linux environment.
 *
 * Features:
 *  - SQLite-backed process registry
 *  - Framework auto-detection
 *  - Real-time CPU/RAM tracking via /proc filesystem (no extra deps)
 *  - Auto-restart on crash (up to MAX_RESTARTS times with backoff)
 */

const { spawn } = require('child_process');
const fs = require('fs');
const fsp = require('fs').promises;
const path = require('path');
const net = require('net');
const Database = require('better-sqlite3');

const PROJECTS_DIR = process.env.PROJECTS_DIR || `${process.env.HOME}/hostpanel-projects`;
const DB_PATH = path.join(PROJECTS_DIR, 'processes.db');

// Max auto-restart attempts before giving up
const MAX_RESTARTS = 5;
// Track restart counts and manual-stop flags per process name
const restartCounts = {};
const manuallyStoppedSet = new Set();

// Ensure projects dir exists
fs.mkdirSync(PROJECTS_DIR, { recursive: true });

// ─── SQLite process registry ──────────────────────────────────────────────────
let db;
function getDb() {
  if (!db) {
    db = new Database(DB_PATH);
    db.exec(`
      CREATE TABLE IF NOT EXISTS processes (
        name        TEXT PRIMARY KEY,
        pid         INTEGER,
        port        INTEGER,
        framework   TEXT,
        status      TEXT DEFAULT 'stopped',
        git_url     TEXT,
        branch      TEXT DEFAULT 'main',
        domain      TEXT,
        log_file    TEXT,
        env_file    TEXT,
        start_cmd   TEXT,
        created_at  TEXT DEFAULT (datetime('now')),
        started_at  TEXT,
        cpu_percent REAL DEFAULT 0,
        mem_mb      REAL DEFAULT 0,
        uptime_sec  INTEGER DEFAULT 0
      )
    `);
  }
  return db;
}

// ─── Framework detection ──────────────────────────────────────────────────────
function detectFramework(projectDir) {
  const pkgPath = path.join(projectDir, 'package.json');
  if (fs.existsSync(pkgPath)) {
    const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'));
    const deps = { ...pkg.dependencies, ...pkg.devDependencies };
    if (deps.next) return 'nextjs';
    if (deps['react-scripts'] || deps['@vitejs/plugin-react']) return 'react';
    return 'nodejs';
  }
  if (fs.existsSync(path.join(projectDir, 'manage.py'))) return 'django';
  if (fs.existsSync(path.join(projectDir, 'requirements.txt'))) return 'python';
  // Spring Boot disabled on Android (compilation too slow/memory-intensive)
  // pom.xml / build.gradle → show warning, not supported
  if (fs.existsSync(path.join(projectDir, 'pom.xml')) ||
      fs.existsSync(path.join(projectDir, 'build.gradle'))) return 'springboot_disabled';
  if (fs.existsSync(path.join(projectDir, 'index.php'))) return 'php';
  return 'static';
}

// ─── Install command per framework ───────────────────────────────────────────
function getInstallCommand(framework, projectDir) {
  switch (framework) {
    case 'nodejs':
    case 'nextjs':
    case 'react':
      return 'npm install --prefer-offline 2>&1';
    case 'django':
    case 'python':
      return 'pip install -r requirements.txt 2>&1';
    case 'static':
    case 'php':
      return 'echo "No dependencies to install"';
    default:
      return 'echo "Skipping install"';
  }
}

// ─── Build command per framework ─────────────────────────────────────────────
function getBuildCommand(framework) {
  switch (framework) {
    case 'nextjs': return 'npm run build 2>&1';
    case 'react':  return 'npm run build 2>&1';
    default:       return null; // no build step needed
  }
}

// ─── Start command per framework ─────────────────────────────────────────────
function getStartCommand(framework, port) {
  switch (framework) {
    case 'nodejs':
      return `node index.js 2>&1 || npm start 2>&1`;
    case 'nextjs':
      return `PORT=${port} npm start 2>&1`;
    case 'react':
      return `npx serve -s build -l ${port} 2>&1`;
    case 'django':
    case 'python':
      return `python manage.py runserver 0.0.0.0:${port} 2>&1`;
    case 'php':
      return `php -S 0.0.0.0:${port} 2>&1`;
    case 'static':
      return `npx serve -s . -l ${port} 2>&1`;
    case 'springboot_disabled':
      throw new Error('Spring Boot is disabled on Android hosting (compilation too slow). Please pre-build your JAR and use the JAR deploy option.');
    default:
      return `npx serve -s . -l ${port} 2>&1`;
  }
}

// ─── Find a free TCP port ─────────────────────────────────────────────────────
async function findFreePort(start = 4000) {
  return new Promise((resolve) => {
    let port = start;
    function tryPort() {
      const srv = net.createServer();
      srv.listen(port, '127.0.0.1', () => {
        srv.close(() => resolve(port));
      });
      srv.on('error', () => { port++; tryPort(); });
    }
    tryPort();
  });
}

// ─── Run a shell command and stream output to log file ───────────────────────
function runCommand(cmd, cwd, logFile) {
  return new Promise((resolve, reject) => {
    const log = fs.createWriteStream(logFile, { flags: 'a' });
    log.write(`\n[${new Date().toISOString()}] $ ${cmd}\n`);
    const proc = spawn('bash', ['-c', cmd], { cwd, env: { ...process.env, CI: 'true' } });
    proc.stdout.on('data', (d) => log.write(d));
    proc.stderr.on('data', (d) => log.write(d));
    proc.on('close', (code) => {
      log.write(`\n[exit code: ${code}]\n`);
      log.close();
      if (code === 0) resolve();
      else reject(new Error(`Command failed (exit ${code}): ${cmd}`));
    });
  });
}

// ─── Spawn persistent process with auto-restart ──────────────────────────────
function spawnProcess(name, cmd, cwd, logFile, envFile) {
  const envVars = loadEnvFile(envFile);
  const proc = spawn('bash', ['-c', cmd], {
    cwd,
    env: { ...process.env, ...envVars },
    detached: false,
    stdio: ['ignore', 'pipe', 'pipe']
  });

  const logStream = fs.createWriteStream(logFile, { flags: 'a' });
  logStream.write(`\n[${new Date().toISOString()}] Starting: ${cmd}\n`);
  proc.stdout.on('data', (d) => logStream.write(d));
  proc.stderr.on('data', (d) => logStream.write(d));

  // Update DB with PID immediately
  try {
    getDb().prepare(
      `UPDATE processes SET pid=?, status='running', started_at=datetime('now') WHERE name=?`
    ).run(proc.pid, name);
  } catch {}

  proc.on('exit', (code) => {
    const ts = new Date().toISOString();
    logStream.write(`\n[${ts}] Process exited with code ${code}\n`);

    // Mark stopped in DB
    try {
      getDb().prepare(`UPDATE processes SET status='stopped', pid=NULL WHERE name=?`).run(name);
    } catch {}

    // ── Auto-restart logic ─────────────────────────────────────────────────
    if (manuallyStoppedSet.has(name)) {
      manuallyStoppedSet.delete(name);
      return; // User explicitly stopped — do NOT restart
    }

    if (code !== 0) {
      const attempts = (restartCounts[name] || 0) + 1;
      restartCounts[name] = attempts;

      if (attempts <= MAX_RESTARTS) {
        const backoffMs = Math.min(attempts * 2000, 30000); // 2s, 4s, 6s … 30s max
        logStream.write(`[${ts}] ⚡ Auto-restarting (attempt ${attempts}/${MAX_RESTARTS}) in ${backoffMs / 1000}s...\n`);
        setTimeout(() => {
          // Make sure it wasn't manually stopped during backoff
          if (!manuallyStoppedSet.has(name)) {
            const newProc = spawnProcess(name, cmd, cwd, logFile, envFile);
            try {
              getDb().prepare(
                `UPDATE processes SET pid=?, status='running', started_at=datetime('now') WHERE name=?`
              ).run(newProc.pid, name);
            } catch {}
          }
        }, backoffMs);
      } else {
        logStream.write(`[${ts}] ❌ Max restarts (${MAX_RESTARTS}) reached. Process is permanently stopped.\n`);
        restartCounts[name] = 0; // Reset for manual restart later
      }
    } else {
      // Clean exit (code 0) — reset counter
      restartCounts[name] = 0;
    }
  });

  return proc;
}

function loadEnvFile(envFile) {
  if (!envFile || !fs.existsSync(envFile)) return {};
  const lines = fs.readFileSync(envFile, 'utf8').split('\n');
  const env = {};
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eqIdx = trimmed.indexOf('=');
    if (eqIdx > 0) {
      env[trimmed.slice(0, eqIdx)] = trimmed.slice(eqIdx + 1);
    }
  }
  return env;
}

// ─── Public API ───────────────────────────────────────────────────────────────

async function listProcesses() {
  const rows = getDb().prepare('SELECT * FROM processes ORDER BY created_at DESC').all();
  return rows.map(formatRow);
}

async function getProcess(name) {
  const row = getDb().prepare('SELECT * FROM processes WHERE name=?').get(name);
  if (!row) throw new Error(`Project '${name}' not found`);
  return formatRow(row);
}

async function startProcess(name) {
  const row = getDb().prepare('SELECT * FROM processes WHERE name=?').get(name);
  if (!row) throw new Error(`Project '${name}' not found`);
  if (row.pid && isAlive(row.pid)) throw new Error(`${name} is already running`);
  // Clear manual-stop flag so auto-restart works again
  manuallyStoppedSet.delete(name);
  restartCounts[name] = 0;
  // spawnProcess updates DB with pid internally
  spawnProcess(name, row.start_cmd, projectDir(name), row.log_file, row.env_file);
}

async function stopProcess(name) {
  const row = getDb().prepare('SELECT * FROM processes WHERE name=?').get(name);
  if (!row || !row.pid) throw new Error(`${name} is not running`);
  // Flag as manually stopped so auto-restart skips it
  manuallyStoppedSet.add(name);
  restartCounts[name] = 0;
  try { process.kill(row.pid, 'SIGTERM'); } catch {}
  getDb().prepare(`UPDATE processes SET status='stopped', pid=NULL WHERE name=?`).run(name);
}

async function restartProcess(name) {
  await stopProcess(name).catch(() => {});
  await new Promise(r => setTimeout(r, 1000));
  await startProcess(name);
}

async function deleteProcess(name) {
  await stopProcess(name).catch(() => {});
  getDb().prepare('DELETE FROM processes WHERE name=?').run(name);
  // Remove project directory
  const dir = projectDir(name);
  if (fs.existsSync(dir)) fs.rmSync(dir, { recursive: true, force: true });
}

async function getEnvVars(name) {
  const row = getDb().prepare('SELECT env_file FROM processes WHERE name=?').get(name);
  if (!row?.env_file || !fs.existsSync(row.env_file)) return {};
  return loadEnvFile(row.env_file);
}

async function updateEnvVars(name, envVars) {
  const row = getDb().prepare('SELECT env_file FROM processes WHERE name=?').get(name);
  if (!row) throw new Error(`Project '${name}' not found`);
  const envFile = row.env_file || path.join(projectDir(name), '.env');
  const content = Object.entries(envVars).map(([k, v]) => `${k}=${v}`).join('\n');
  fs.writeFileSync(envFile, content + '\n');
  getDb().prepare('UPDATE processes SET env_file=? WHERE name=?').run(envFile, name);
}

async function rebuildProcess(name) {
  const row = getDb().prepare('SELECT * FROM processes WHERE name=?').get(name);
  if (!row) throw new Error(`Project '${name}' not found`);

  await stopProcess(name).catch(() => {});

  const dir = projectDir(name);
  const logFile = row.log_file;

  // git pull
  await runCommand('git pull origin ' + (row.branch || 'main'), dir, logFile);

  // reinstall + build
  const installCmd = getInstallCommand(row.framework, dir);
  await runCommand(installCmd, dir, logFile);

  const buildCmd = getBuildCommand(row.framework);
  if (buildCmd) await runCommand(buildCmd, dir, logFile);

  // restart
  await startProcess(name);
}

// ─── Register a new project (called from deploy route) ───────────────────────
async function registerProcess({ name, gitUrl, branch, framework, port, domain, envVars }) {
  const dir = projectDir(name);
  const logFile = path.join(dir, 'hostpanel.log');
  const envFile = path.join(dir, '.env');

  if (envVars && Object.keys(envVars).length > 0) {
    fs.mkdirSync(dir, { recursive: true });
    const content = Object.entries(envVars).map(([k, v]) => `${k}=${v}`).join('\n');
    fs.writeFileSync(envFile, content + '\n');
  }

  const startCmd = getStartCommand(framework, port);

  getDb().prepare(`
    INSERT OR REPLACE INTO processes
      (name, pid, port, framework, status, git_url, branch, domain, log_file, env_file, start_cmd)
    VALUES (?, NULL, ?, ?, 'stopped', ?, ?, ?, ?, ?, ?)
  `).run(name, port, framework, gitUrl, branch || 'main', domain || null, logFile, envFile, startCmd);
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
function projectDir(name) {
  return path.join(PROJECTS_DIR, name);
}

function isAlive(pid) {
  try { process.kill(pid, 0); return true; } catch { return false; }
}

// ─── CPU/RAM tracking via /proc (no extra npm deps) ──────────────────────────
// Reads /proc/{pid}/stat and /proc/{pid}/status to get CPU ticks and RSS memory.
const prevCpuTicks = {}; // { name: { utime, stime, timestamp } }

function readProcStat(pid) {
  try {
    const stat = fs.readFileSync(`/proc/${pid}/stat`, 'utf8').split(' ');
    return {
      utime: parseInt(stat[13]) || 0,
      stime: parseInt(stat[14]) || 0
    };
  } catch { return null; }
}

function readProcMemKb(pid) {
  try {
    const status = fs.readFileSync(`/proc/${pid}/status`, 'utf8');
    const match = status.match(/VmRSS:\s*(\d+)\s*kB/);
    return match ? parseInt(match[1]) : 0;
  } catch { return 0; }
}

// Background ticker: update cpu_percent + mem_mb every 10 seconds
function startMetricsTicker() {
  setInterval(() => {
    try {
      const rows = getDb().prepare(`SELECT name, pid FROM processes WHERE pid IS NOT NULL`).all();
      const CLK_TCK = 100; // standard Linux HZ
      const now = Date.now();

      for (const row of rows) {
        const { name, pid } = row;
        if (!pid || !isAlive(pid)) continue;

        const stat = readProcStat(pid);
        const memKb = readProcMemKb(pid);
        const memMb = parseFloat((memKb / 1024).toFixed(1));

        let cpuPercent = 0;
        if (stat) {
          const totalTicks = stat.utime + stat.stime;
          const prev = prevCpuTicks[name];
          if (prev) {
            const tickDelta = totalTicks - prev.totalTicks;
            const timeDeltaSec = (now - prev.timestamp) / 1000;
            cpuPercent = parseFloat(((tickDelta / CLK_TCK / timeDeltaSec) * 100).toFixed(1));
          }
          prevCpuTicks[name] = { totalTicks, timestamp: now };
        }

        getDb().prepare(
          `UPDATE processes SET cpu_percent=?, mem_mb=? WHERE name=?`
        ).run(cpuPercent, memMb, name);
      }
    } catch { /* silent — don't crash on metrics errors */ }
  }, 10_000);
}

// Start ticker when module loads
startMetricsTicker();

function formatRow(row) {
  const alive = row.pid && isAlive(row.pid);
  // Sync status to DB if process died externally (e.g. OOM kill)
  if (!alive && row.status === 'running') {
    try {
      getDb().prepare(`UPDATE processes SET status='stopped', pid=NULL WHERE name=?`).run(row.name);
    } catch {}
  }
  return {
    id: row.name,
    name: row.name,
    status: alive ? 'running' : 'stopped',
    framework: row.framework || 'static',
    ports: row.port ? `${row.port}` : '',
    customDomain: row.domain || null,
    gitUrl: row.git_url || null,
    branch: row.branch || 'main',
    image: `${row.framework}:local`,
    created: row.created_at ? row.created_at.split(' ')[0] : '',
    uptime: row.started_at || '',
    cpuPercent: row.cpu_percent || 0,
    memoryUsageMb: row.mem_mb || 0,
    sslEnabled: false,
    localUrl: row.port ? `http://localhost:${row.port}` : null
  };
}

module.exports = {
  listProcesses, getProcess, startProcess, stopProcess,
  restartProcess, deleteProcess, getEnvVars, updateEnvVars,
  rebuildProcess, registerProcess, detectFramework,
  getInstallCommand, getBuildCommand, getStartCommand,
  findFreePort, runCommand, spawnProcess, projectDir
};
