const router = require('express').Router();
const auth = require('../middleware/auth');
const { getAuditLogs } = require('../services/auditService');

router.get('/audit-logs', auth, (req, res) => {
  try {
    const limit = parseInt(req.query.limit) || 100;
    const logs = getAuditLogs(limit);
    res.json(logs);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
