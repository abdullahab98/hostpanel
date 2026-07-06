/**
 * routes/domains.js
 * Domain management for Android-native hosting.
 * Since we're on Termux (no Caddy), domains are stored as metadata
 * linked to tunnel URLs. Custom HTTPS domains require an external proxy.
 */

const router = require('express').Router();
const auth = require('../middleware/auth');
const svc = require('../services/processService');
const { logAudit } = require('../services/auditService');

// GET /api/domains — list all projects with custom domains configured
router.get('/domains', auth, async (req, res) => {
  try {
    const processes = await svc.listProcesses();
    const domains = processes
      .filter(p => p.customDomain)
      .map(p => ({
        id: p.name,
        domain: p.customDomain,
        projectName: p.name,
        sslStatus: 'pending',    // Termux cannot auto-provision SSL — tunnel provides HTTPS
        sslExpiry: null,
        autoRenew: false,
        createdAt: p.created,
        dnsVerified: false,
        localUrl: p.localUrl
      }));
    res.json(domains);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// POST /api/domains — set a custom domain for an existing project
router.post('/domains', auth, async (req, res) => {
  const { domain, projectName } = req.body;
  if (!domain || !projectName) {
    return res.status(400).json({ error: 'domain and projectName are required' });
  }
  try {
    // Use processService's JSON store directly
    const JsonStore = require('../services/jsonStore');
    const path = require('path');
    const dbPath = path.join(process.env.PROJECTS_DIR || `${process.env.HOME}/hostpanel-projects`, 'processes.db');
    const store = new JsonStore(dbPath);
    store.prepare('UPDATE processes SET domain=? WHERE name=?').run(domain, projectName);
    store.close();

    await logAudit('domain_add', domain, `Linked to project: ${projectName}`);
    res.json({
      id: projectName,
      domain,
      projectName,
      sslStatus: 'pending',
      message: `Domain ${domain} linked to ${projectName}. Point your DNS to your tunnel URL for HTTPS access.`
    });
  } catch (err) {
    await logAudit('domain_add', domain, `Failed: ${err.message}`, 'FAILURE');
    res.status(500).json({ error: err.message });
  }
});

// DELETE /api/domains/:domain — remove a custom domain
router.delete('/domains/:domain', auth, async (req, res) => {
  try {
    const JsonStore = require('../services/jsonStore');
    const path = require('path');
    const dbPath = path.join(process.env.PROJECTS_DIR || `${process.env.HOME}/hostpanel-projects`, 'processes.db');
    const store = new JsonStore(dbPath);
    store.prepare("UPDATE processes SET domain=NULL WHERE domain=?").run(req.params.domain);
    store.close();

    await logAudit('domain_remove', req.params.domain, 'Domain removed');
    res.json({ success: true, message: `${req.params.domain} removed` });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// POST /api/domains/:domain/verify — DNS check (simple HTTP ping)
router.post('/domains/:domain/verify', auth, async (req, res) => {
  const domain = req.params.domain;
  try {
    const dns = require('dns').promises;
    const records = await dns.lookup(domain);
    res.json({
      verified: true,
      message: `DNS resolves to ${records.address}`,
      resolvedIp: records.address
    });
  } catch (err) {
    res.json({
      verified: false,
      message: `DNS lookup failed: ${err.message}`,
      resolvedIp: null
    });
  }
});

// POST /api/domains/:domain/ssl/renew — SSL not applicable on Termux, return info
router.post('/domains/:domain/ssl/renew', auth, (req, res) => {
  res.json({
    success: false,
    message: 'SSL auto-renewal is not available on Android-native hosting. Use your Cloudflare/ngrok tunnel URL for HTTPS access.'
  });
});

module.exports = router;
