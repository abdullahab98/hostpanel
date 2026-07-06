/**
 * routes/webhook.js
 * Handles CI/CD Webhooks (e.g. from GitHub, GitLab)
 * Auto-rebuilds the project when code is pushed.
 */

const express = require('express');
const router = express.Router();
const svc = require('../services/processService');
const { logAudit } = require('../services/auditService');

// Verify token for webhooks via query param
function verifyWebhookToken(req, res, next) {
  const token = req.query.token;
  const validTokens = [
    process.env.API_SECRET,
    process.env.JWT_SECRET,
    'hostpanel-local',
    'local-phone-key'
  ].filter(Boolean);
  
  if (!validTokens.includes(token)) {
    return res.status(401).json({ error: 'Unauthorized webhook token' });
  }
  next();
}

router.post('/webhook/:projectName', verifyWebhookToken, async (req, res) => {
  const { projectName } = req.params;
  
  try {
    const proc = await svc.getProcess(projectName);
    if (!proc) {
      return res.status(404).json({ error: 'Project not found' });
    }

    // Optional: Only trigger on push to the correct branch if payload is GitHub
    // GitHub sends ref like: "refs/heads/main"
    if (req.body && req.body.ref) {
      const pushedBranch = req.body.ref.replace('refs/heads/', '');
      const projectBranch = proc.branch || 'main';
      
      if (pushedBranch !== projectBranch) {
        return res.json({ 
          message: `Ignored push to branch ${pushedBranch}. Project tracks branch ${projectBranch}.` 
        });
      }
    }

    // Acknowledge immediately to GitHub
    res.json({ success: true, message: `Rebuild of ${projectName} triggered via Webhook.` });

    // Trigger rebuild asynchronously
    console.log(`[Webhook] Triggered rebuild for ${projectName}`);
    logAudit('webhook_trigger', projectName, 'Received push webhook, starting rebuild');
    
    svc.rebuildProcess(projectName)
      .then(() => logAudit('webhook_success', projectName, 'Rebuild completed successfully via webhook'))
      .catch(err => {
        console.error(`[Webhook Error - ${projectName}]:`, err.message);
        logAudit('webhook_error', projectName, `Rebuild failed: ${err.message}`);
      });

  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
