/**
 * routes/databases.js
 * API for managing native databases inside Termux (MariaDB, PostgreSQL, Redis)
 */

const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const dbSvc = require('../services/dbService');
const { logAudit } = require('../services/auditService');

// Get all databases and their status
router.get('/databases', auth, async (req, res) => {
  try {
    const list = await dbSvc.listDatabases();
    res.json(list);
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// Install a database package
router.post('/databases/:id/install', auth, async (req, res) => {
  const { id } = req.params;
  try {
    res.json({ success: true, message: `Installation of ${id} started in background.` });
    
    // Run install asynchronously to avoid blocking the HTTP response
    setTimeout(async () => {
      try {
        await dbSvc.installDb(id);
        logAudit('db_install', id, `Installed database package: ${id}`);
      } catch (err) {
        console.error(`Failed to install DB ${id}:`, err);
      }
    }, 100);

  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// Start a database service
router.post('/databases/:id/start', auth, async (req, res) => {
  const { id } = req.params;
  try {
    await dbSvc.startDb(id);
    logAudit('db_start', id, `Started database service: ${id}`);
    res.json({ success: true, message: `${id} started successfully` });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// Stop a database service
router.post('/databases/:id/stop', auth, async (req, res) => {
  const { id } = req.params;
  try {
    await dbSvc.stopDb(id);
    logAudit('db_stop', id, `Stopped database service: ${id}`);
    res.json({ success: true, message: `${id} stopped successfully` });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;
