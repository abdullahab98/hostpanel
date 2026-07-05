require('dotenv').config();
const express = require('express');
const cors = require('cors');
const http = require('http');
const { WebSocketServer } = require('ws');
const rateLimit = require('express-rate-limit');
const { initDb } = require('./services/auditService');
const { setupTerminalWebSocket } = require('./routes/terminal');

const app = express();
const server = http.createServer(app);
const wss = new WebSocketServer({ server });

// Middleware
app.use(cors());
app.use(express.json());

// Rate limiting
const limiter = rateLimit({
  windowMs: 60 * 1000,
  max: 60,
  message: { error: 'Too many requests. Max 60/minute.' }
});
app.use('/api/', limiter);

// Routes
app.use('/api', require('./routes/metrics'));
app.use('/api', require('./routes/containers')); // Note: now manages native processes
app.use('/api', require('./routes/deploy'));
app.use('/api', require('./routes/domains'));    // Kept for UI compatibility, but custom domains now use tunnel URL logic
app.use('/api', require('./routes/logs')(wss));
app.use('/api', require('./routes/audit'));
app.use('/api', require('./routes/tunnel'));
app.use('/api', require('./routes/databases'));
app.use('/api', require('./routes/webhook'));

// Setup Terminal WebSocket directly on wss
setupTerminalWebSocket(wss);

// Health check (no auth required)
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', version: '3.0.0', time: new Date().toISOString() });
});

// 404 handler
app.use((req, res) => res.status(404).json({ error: 'Endpoint not found' }));

// Global error handler
app.use((err, req, res, next) => {
  console.error('Unhandled error:', err);
  res.status(500).json({ error: 'Internal server error', details: err.message });
});

const PORT = process.env.PORT || 3001;

// Initialize DB then start
initDb().then(() => {
  server.listen(PORT, () => {
    console.log(`\n🚀 HostPanel Control Plane v3.0 (Android Native)`);
    console.log(`   Listening on port ${PORT}`);
    console.log(`   Projects dir: ${process.env.PROJECTS_DIR || '~/hostpanel-projects'}\n`);
  });
}).catch(err => {
  console.error('Failed to initialize database:', err);
  process.exit(1);
});
