/**
 * routes/containers.js — Process management (no Docker)
 * CRUD + start/stop/restart via processService
 */

const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const { logAudit } = require('../services/auditService');
const svc = require('../services/processService');

// List all projects
router.get('/containers', auth, async (req, res) => {
  try {
    const processes = await svc.listProcesses();
    res.json(processes);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Get single project
router.get('/containers/:name', auth, async (req, res) => {
  try {
    const proc = await svc.getProcess(req.params.name);
    res.json(proc);
  } catch (err) {
    res.status(404).json({ error: err.message });
  }
});

// Start
router.post('/containers/:name/start', auth, async (req, res) => {
  try {
    await svc.startProcess(req.params.name);
    logAudit('start', req.params.name, 'Process started');
    res.json({ success: true, message: `${req.params.name} started` });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// Stop
router.post('/containers/:name/stop', auth, async (req, res) => {
  try {
    await svc.stopProcess(req.params.name);
    logAudit('stop', req.params.name, 'Process stopped');
    res.json({ success: true, message: `${req.params.name} stopped` });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// Restart
router.post('/containers/:name/restart', auth, async (req, res) => {
  try {
    await svc.restartProcess(req.params.name);
    logAudit('restart', req.params.name, 'Process restarted');
    res.json({ success: true, message: `${req.params.name} restarted` });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// Rebuild (git pull + reinstall + restart)
router.post('/containers/:name/rebuild', auth, async (req, res) => {
  res.json({ success: true, message: `Rebuild of ${req.params.name} started. Check logs.` });
  svc.rebuildProcess(req.params.name)
    .then(() => logAudit('rebuild', req.params.name, 'Rebuild complete'))
    .catch(err => console.error(`Rebuild error [${req.params.name}]:`, err.message));
});

// Delete
router.delete('/containers/:name', auth, async (req, res) => {
  try {
    await svc.deleteProcess(req.params.name);
    logAudit('delete', req.params.name, 'Project deleted');
    res.json({ success: true, message: `${req.params.name} deleted` });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// Get env vars
router.get('/containers/:name/env', auth, async (req, res) => {
  try {
    const vars = await svc.getEnvVars(req.params.name);
    res.json(vars);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Update env vars
router.put('/containers/:name/env', auth, async (req, res) => {
  try {
    await svc.updateEnvVars(req.params.name, req.body);
    res.json({ success: true, message: 'Environment variables updated. Restart to apply.' });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;
