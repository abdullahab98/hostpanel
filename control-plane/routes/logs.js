/**
 * routes/logs.js — Real-time log streaming via WebSocket
 * Tails log files from the Termux filesystem (no Docker needed).
 */

const express = require('express');
const router = express.Router();
const fs = require('fs');
const path = require('path');
const auth = require('../middleware/auth');
const { verifyToken } = require('../middleware/auth');
const svc = require('../services/processService');

const TAIL_LINES = 100;

// HTTP endpoint: last N lines
router.get('/containers/:name/logs', auth, (req, res) => {
  try {
    const logFile = path.join(svc.projectDir(req.params.name), 'hostpanel.log');
    if (!fs.existsSync(logFile)) return res.json({ logs: [] });

    const content = fs.readFileSync(logFile, 'utf8');
    const lines = content.split('\n').filter(Boolean);
    res.json({ logs: lines.slice(-TAIL_LINES) });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// WebSocket log streaming — called from index.js with the wss instance
function setupLogWebSocket(wss) {
  wss.on('connection', (ws, req) => {
    const url = new URL(req.url, 'http://localhost');

    // Route: /ws/logs/:name
    const logsMatch = url.pathname.match(/^\/ws\/logs\/(.+)$/);
    // Route: /ws/terminal
    const terminalMatch = url.pathname === '/ws/terminal';

    if (!logsMatch && !terminalMatch) return;

    // Auth via query param token
    const token = url.searchParams.get('token');
    if (!verifyToken(token)) {
      ws.send(JSON.stringify({ error: 'Unauthorized' }));
      ws.close(1008, 'Unauthorized');
      return;
    }

    if (logsMatch) {
      setupLogTail(ws, logsMatch[1]);
    }
    // terminal is handled in terminal.js
  });
}

function setupLogTail(ws, projectName) {
  const logFile = path.join(svc.projectDir(projectName), 'hostpanel.log');

  // Send existing content first
  if (fs.existsSync(logFile)) {
    const existing = fs.readFileSync(logFile, 'utf8');
    const lines = existing.split('\n').filter(Boolean).slice(-TAIL_LINES);
    for (const line of lines) ws.send(line);
  }

  // Watch for new writes
  let watcher = null;
  let lastSize = fs.existsSync(logFile) ? fs.statSync(logFile).size : 0;

  const poll = setInterval(() => {
    if (!fs.existsSync(logFile)) return;
    const stat = fs.statSync(logFile);
    if (stat.size > lastSize) {
      const fd = fs.openSync(logFile, 'r');
      const buf = Buffer.alloc(stat.size - lastSize);
      fs.readSync(fd, buf, 0, buf.length, lastSize);
      fs.closeSync(fd);
      lastSize = stat.size;
      const newContent = buf.toString('utf8');
      for (const line of newContent.split('\n').filter(Boolean)) {
        if (ws.readyState === ws.OPEN) ws.send(line);
      }
    }
  }, 500);

  ws.on('close', () => clearInterval(poll));
  ws.on('error', () => clearInterval(poll));
}

module.exports = (wss) => {
  setupLogWebSocket(wss);
  return router;
};
