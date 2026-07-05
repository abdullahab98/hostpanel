/**
 * dbService.js
 * Manages native database processes in Termux (MariaDB, PostgreSQL, Redis)
 */

const { spawn, execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const Database = require('better-sqlite3');

const DB_DATA_DIR = process.env.DB_DATA_DIR || `${process.env.HOME}/hostpanel-dbs`;
const SQLITE_DB_PATH = path.join(process.env.PROJECTS_DIR || `${process.env.HOME}/hostpanel-projects`, 'processes.db');

fs.mkdirSync(DB_DATA_DIR, { recursive: true });

// We share the same processes.db for audit or state if needed, but for DBs we can track dynamically 
// or just rely on process checking (pgrep/netstat).
function getSQLite() {
  return new Database(SQLITE_DB_PATH);
}

const SUPPORTED_DBS = {
  mariadb: {
    name: 'MariaDB (MySQL)',
    port: 3306,
    installCmd: 'pkg install -y mariadb',
    checkCmd: 'command -v mysqld',
    startCmd: `mysqld_safe --datadir=${DB_DATA_DIR}/mariadb > ${DB_DATA_DIR}/mariadb.log 2>&1 &`,
    stopCmd: 'pkill -f mysqld',
    initCmd: `mysql_install_db --datadir=${DB_DATA_DIR}/mariadb`
  },
  postgresql: {
    name: 'PostgreSQL',
    port: 5432,
    installCmd: 'pkg install -y postgresql',
    checkCmd: 'command -v postgres',
    startCmd: `pg_ctl -D ${DB_DATA_DIR}/postgres -l ${DB_DATA_DIR}/postgres.log start`,
    stopCmd: `pg_ctl -D ${DB_DATA_DIR}/postgres stop`,
    initCmd: `initdb -D ${DB_DATA_DIR}/postgres`
  },
  redis: {
    name: 'Redis',
    port: 6379,
    installCmd: 'pkg install -y redis',
    checkCmd: 'command -v redis-server',
    startCmd: `redis-server --daemonize yes --logfile ${DB_DATA_DIR}/redis.log --dir ${DB_DATA_DIR}`,
    stopCmd: 'pkill -f redis-server',
    initCmd: null
  }
};

function isInstalled(dbId) {
  const db = SUPPORTED_DBS[dbId];
  if (!db) return false;
  try {
    execSync(db.checkCmd, { stdio: 'ignore' });
    return true;
  } catch {
    return false;
  }
}

function isRunning(dbId) {
  const db = SUPPORTED_DBS[dbId];
  if (!db) return false;
  try {
    // Check if the port is bound by a process
    const out = execSync('netstat -lnt').toString();
    return out.includes(`:${db.port} `) || out.includes(`:${db.port}\n`);
  } catch {
    return false;
  }
}

async function installDb(dbId) {
  const db = SUPPORTED_DBS[dbId];
  if (!db) throw new Error('Unsupported database');
  if (isInstalled(dbId)) return; // Already installed

  // Install package
  execSync(db.installCmd, { stdio: 'inherit' });

  // Initialize data dir if required
  if (db.initCmd) {
    const dataDir = path.join(DB_DATA_DIR, dbId === 'postgresql' ? 'postgres' : dbId);
    if (!fs.existsSync(dataDir)) {
      fs.mkdirSync(dataDir, { recursive: true });
      execSync(db.initCmd, { stdio: 'inherit' });
    }
  }
}

async function startDb(dbId) {
  const db = SUPPORTED_DBS[dbId];
  if (!db) throw new Error('Unsupported database');
  if (!isInstalled(dbId)) throw new Error(`${db.name} is not installed`);
  if (isRunning(dbId)) return;

  execSync(db.startCmd);
}

async function stopDb(dbId) {
  const db = SUPPORTED_DBS[dbId];
  if (!db) throw new Error('Unsupported database');
  if (!isRunning(dbId)) return;

  execSync(db.stopCmd);
}

async function listDatabases() {
  const results = [];
  for (const [id, db] of Object.entries(SUPPORTED_DBS)) {
    const installed = isInstalled(id);
    const running = installed ? isRunning(id) : false;
    
    // Connection string based on localhost since we're in Termux
    let connectionString = null;
    if (installed) {
      if (id === 'mariadb') connectionString = `mysql://root@localhost:3306`;
      else if (id === 'postgresql') connectionString = `postgres://$(whoami)@localhost:5432`;
      else if (id === 'redis') connectionString = `redis://localhost:6379`;
    }

    results.push({
      id,
      name: db.name,
      port: db.port,
      isInstalled: installed,
      isRunning: running,
      connectionString
    });
  }
  return results;
}

module.exports = {
  SUPPORTED_DBS,
  isInstalled,
  isRunning,
  installDb,
  startDb,
  stopDb,
  listDatabases
};
