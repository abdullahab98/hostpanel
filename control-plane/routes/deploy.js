/**
 * routes/deploy.js — Android-native deploy (no Docker)
 * git clone → install → build → spawn process
 */

const express = require('express');
const router = express.Router();
const path = require('path');
const fs = require('fs');
const simpleGit = require('simple-git');
const auth = require('../middleware/auth');
const { logAudit } = require('../services/auditService');
const svc = require('../services/processService');

const PROJECTS_DIR = process.env.PROJECTS_DIR || `${process.env.HOME}/hostpanel-projects`;

router.post('/deploy', auth, async (req, res) => {
  const { projectName, gitUrl, branch = 'main', framework, customDomain, targetPort, envVars = {} } = req.body;

  if (!projectName || !gitUrl || !framework) {
    return res.status(400).json({ success: false, message: 'projectName, gitUrl, and framework are required' });
  }

  // Spring Boot explicitly disabled
  if (framework === 'springboot' || framework === 'springboot_disabled') {
    return res.status(400).json({
      success: false,
      message: '⚠️ Spring Boot is disabled on Android hosting. Java compilation is too slow and memory-intensive for mobile devices. Please use Node.js, Python, or Static projects.'
    });
  }

  const projectDir = svc.projectDir(projectName);
  const logFile = path.join(projectDir, 'hostpanel.log');

  // Respond immediately — clone+install happens async
  res.json({
    success: true,
    message: `Deployment of ${projectName} started. Stream logs at /api/deploy/${projectName}/logs/ws`,
    projectId: projectName,
    url: customDomain ? `http://${customDomain}` : null
  });

  // Async deploy pipeline
  (async () => {
    try {
      fs.mkdirSync(projectDir, { recursive: true });
      const log = (msg) => fs.appendFileSync(logFile, `[${new Date().toISOString()}] ${msg}\n`);

      log(`🚀 Starting deployment of ${projectName}`);
      log(`📦 Framework: ${framework}`);
      log(`🔗 Git URL: ${gitUrl}@${branch}`);

      // ── Step 1: git clone or pull ──────────────────────────────────────────
      if (fs.existsSync(path.join(projectDir, '.git'))) {
        log('📥 Pulling latest changes...');
        await simpleGit(projectDir).pull('origin', branch);
      } else {
        log('📥 Cloning repository...');
        await simpleGit().clone(gitUrl, projectDir, ['--branch', branch, '--depth', '1']);
      }
      log('✅ Code ready');

      // ── Step 2: Auto-detect framework if needed ────────────────────────────
      const detectedFramework = framework === 'auto'
        ? svc.detectFramework(projectDir)
        : framework;

      if (detectedFramework === 'springboot_disabled') {
        log('❌ Spring Boot detected — disabled on Android. Deploy cancelled.');
        return;
      }

      // ── Step 3: Find free port ─────────────────────────────────────────────
      const port = targetPort || await svc.findFreePort(4000);
      log(`🔌 Using port ${port}`);

      // ── Step 4: Install dependencies ───────────────────────────────────────
      const installCmd = svc.getInstallCommand(detectedFramework, projectDir);
      log(`📦 Installing: ${installCmd}`);
      await svc.runCommand(installCmd, projectDir, logFile);
      log('✅ Dependencies installed');

      // ── Step 5: Build (if needed) ──────────────────────────────────────────
      const buildCmd = svc.getBuildCommand(detectedFramework);
      if (buildCmd) {
        log(`🔨 Building: ${buildCmd}`);
        await svc.runCommand(buildCmd, projectDir, logFile);
        log('✅ Build complete');
      }

      // ── Step 6: Write .env file ────────────────────────────────────────────
      if (Object.keys(envVars).length > 0) {
        const envContent = Object.entries(envVars).map(([k, v]) => `${k}=${v}`).join('\n');
        fs.writeFileSync(path.join(projectDir, '.env'), envContent + '\n');
        log(`✅ Environment variables written (${Object.keys(envVars).length} vars)`);
      }

      // ── Step 7: Register & start process ──────────────────────────────────
      await svc.registerProcess({
        name: projectName,
        gitUrl, branch,
        framework: detectedFramework,
        port,
        domain: customDomain || null,
        envVars
      });

      await svc.startProcess(projectName);
      log(`✅ [SUCCESS] ${projectName} is running on http://localhost:${port}`);
      if (customDomain) log(`🌐 Custom domain: http://${customDomain}`);

      logAudit('deploy', projectName, `Deployed ${detectedFramework} project on port ${port}`);

    } catch (err) {
      const log = (msg) => fs.appendFileSync(logFile, `[${new Date().toISOString()}] ${msg}\n`);
      log(`❌ Deploy failed: ${err.message}`);
      console.error(`[${projectName}] Deploy error:`, err.message);
    }
  })();
});

// Stream deploy logs via WebSocket (handled in logs route)
router.get('/deploy/:name/logs', auth, (req, res) => {
  const logFile = path.join(svc.projectDir(req.params.name), 'hostpanel.log');
  if (!fs.existsSync(logFile)) return res.json({ logs: [] });
  const content = fs.readFileSync(logFile, 'utf8');
  res.json({ logs: content.split('\n').filter(Boolean) });
});

module.exports = router;
