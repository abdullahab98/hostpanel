/**
 * routes/terminal.js — WebSocket PTY terminal
 * Gives the Android app a full bash shell via xterm.js WebSocket.
 * Uses node-pty to spawn a real PTY process in Termux.
 */

const { verifyToken } = require('../middleware/auth');

let pty;
try {
  pty = require('node-pty');
} catch (e) {
  console.warn('⚠️  node-pty not available — terminal feature disabled. Run: npm install node-pty');
}

// Called from index.js with the WebSocketServer instance
function setupTerminalWebSocket(wss) {
  if (!pty) return;

  wss.on('connection', (ws, req) => {
    const url = new URL(req.url, 'http://localhost');
    if (url.pathname !== '/ws/terminal') return;

    // Auth
    const token = url.searchParams.get('token');
    if (!verifyToken(token)) {
      ws.send('\r\n\x1b[31mUnauthorized — invalid token\x1b[0m\r\n');
      ws.close(1008, 'Unauthorized');
      return;
    }

    // Spawn bash inside Termux
    const shell = process.env.SHELL || '/data/data/com.termux/files/usr/bin/bash';
    const cols = parseInt(url.searchParams.get('cols') || '80');
    const rows = parseInt(url.searchParams.get('rows') || '24');

    let term;
    try {
      term = pty.spawn(shell, [], {
        name: 'xterm-256color',
        cols,
        rows,
        cwd: process.env.HOME || '/data/data/com.termux/files/home',
        env: process.env
      });
    } catch (err) {
      ws.send(`\r\n\x1b[31mFailed to spawn terminal: ${err.message}\x1b[0m\r\n`);
      ws.close();
      return;
    }

    // PTY → WebSocket
    term.onData((data) => {
      if (ws.readyState === ws.OPEN) ws.send(data);
    });

    term.onExit(() => {
      if (ws.readyState === ws.OPEN) {
        ws.send('\r\n\x1b[33m[session ended]\x1b[0m\r\n');
        ws.close();
      }
    });

    // WebSocket → PTY
    ws.on('message', (msg) => {
      try {
        const data = JSON.parse(msg);
        if (data.type === 'input') {
          term.write(data.data);
        } else if (data.type === 'resize') {
          term.resize(data.cols || cols, data.rows || rows);
        }
      } catch {
        // Plain text input
        term.write(msg.toString());
      }
    });

    ws.on('close', () => {
      try { term.kill(); } catch {}
    });

    ws.on('error', () => {
      try { term.kill(); } catch {}
    });

    // Welcome message
    ws.send(`\r\n\x1b[36m╔══════════════════════════════════════╗\x1b[0m\r\n`);
    ws.send(`\x1b[36m║   HostPanel Terminal — Termux Shell  ║\x1b[0m\r\n`);
    ws.send(`\x1b[36m╚══════════════════════════════════════╝\x1b[0m\r\n\r\n`);
  });
}

module.exports = { setupTerminalWebSocket };
