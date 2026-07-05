/**
 * routes/tunnel.js — Tunnel management API
 * Start/stop ngrok or Cloudflare Quick Tunnel from within the Android app.
 */

const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const tunnelSvc = require('../services/tunnelService');
const { logAudit } = require('../services/auditService');

// Get current tunnel status
router.get('/tunnel', auth, (req, res) => {
  res.json(tunnelSvc.getTunnelStatus());
});

// Start tunnel
router.post('/tunnel/start', auth, async (req, res) => {
  const { port = 3001, provider = 'cloudflare' } = req.body;
  try {
    const url = await tunnelSvc.startTunnel(port, provider);
    if (!url) {
      return res.status(500).json({ success: false, message: 'Tunnel started but URL was not received within 30s. Check Termux logs.' });
    }
    logAudit('tunnel_start', 'system', `${provider} tunnel started → ${url}`);
    res.json({ success: true, url, provider });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// Stop tunnel
router.post('/tunnel/stop', auth, async (req, res) => {
  tunnelSvc.stopTunnel();
  logAudit('tunnel_stop', 'system', 'Tunnel stopped');
  res.json({ success: true, message: 'Tunnel stopped' });
});

// Get just the URL (lightweight ping from app)
router.get('/tunnel/url', auth, (req, res) => {
  const status = tunnelSvc.getTunnelStatus();
  res.json({ url: status.url, running: status.running });
});

module.exports = router;
