/**
 * auditService.js
 * Immutable audit log writer using SQLite (better-sqlite3).
 * 
 * logAudit(action, target, details, status?)
 *   - action  : what happened (e.g. 'deploy', 'start', 'webhook_trigger')
 *   - target  : which project/resource was affected
 *   - details : human-readable description
 *   - status  : 'SUCCESS' | 'FAILURE' | 'INFO' (defaults to 'SUCCESS')
 */

const Database = require('better-sqlite3');
const path = require('path');
const { randomUUID } = require('crypto');

const PROJECTS_DIR = process.env.PROJECTS_DIR || `${process.env.HOME}/hostpanel-projects`;
const DB_PATH = path.join(PROJECTS_DIR, 'audit.db');
let db;

async function initDb() {
  const fs = require('fs');
  fs.mkdirSync(PROJECTS_DIR, { recursive: true });

  db = new Database(DB_PATH);
  db.exec(`
    CREATE TABLE IF NOT EXISTS audit_logs (
      id        TEXT PRIMARY KEY,
      timestamp TEXT NOT NULL,
      action    TEXT NOT NULL,
      target    TEXT NOT NULL,
      status    TEXT NOT NULL DEFAULT 'SUCCESS',
      details   TEXT DEFAULT ''
    );
    CREATE INDEX IF NOT EXISTS idx_timestamp ON audit_logs(timestamp DESC);
  `);
  console.log('✅ Audit DB initialized at', DB_PATH);
}

/**
 * Write an audit log entry.
 * @param {string} action  - e.g. 'deploy', 'start', 'stop', 'webhook_trigger'
 * @param {string} target  - project/resource name
 * @param {string} details - human-readable message
 * @param {string} status  - 'SUCCESS' | 'FAILURE' | 'INFO'
 */
function logAudit(action, target, details = '', status = 'SUCCESS') {
  if (!db) return;
  try {
    db.prepare(
      'INSERT INTO audit_logs (id, timestamp, action, target, status, details) VALUES (?, ?, ?, ?, ?, ?)'
    ).run(randomUUID(), new Date().toISOString(), action, target, status, details);
  } catch (err) {
    console.error('Audit log error:', err.message);
  }
}

/**
 * Retrieve recent audit log entries.
 * @param {number} limit - max number of rows (default 100)
 * @returns {Array}
 */
function getAuditLogs(limit = 100) {
  if (!db) return [];
  return db.prepare(
    'SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?'
  ).all(limit);
}

module.exports = { initDb, logAudit, getAuditLogs };
