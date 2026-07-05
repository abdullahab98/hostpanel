/**
 * routes/metrics.js — System metrics (Android/Termux native)
 * Removed Docker dependency — uses systeminformation + native processService.
 */
const router = require('express').Router();
const auth = require('../middleware/auth');
const si = require('systeminformation');
const svc = require('../services/processService');

router.get('/metrics', auth, async (req, res) => {
  try {
    const [cpu, mem, disk, net, osInfo, time] = await Promise.all([
      si.currentLoad(),
      si.mem(),
      si.fsSize(),
      si.networkStats(),
      si.osInfo(),
      si.time()
    ]);

    // Count running native processes (replaces Docker container count)
    const processes = await svc.listProcesses();
    const activeCount = processes.filter(p => p.status === 'running').length;

    res.json({
      cpuUsagePercent: parseFloat(cpu.currentLoad.toFixed(1)),
      ramTotalGb: parseFloat((mem.total / 1e9).toFixed(2)),
      ramUsedGb: parseFloat((mem.active / 1e9).toFixed(2)),
      diskTotalGb: parseFloat(((disk[0]?.size || 0) / 1e9).toFixed(1)),
      diskUsedGb: parseFloat(((disk[0]?.used || 0) / 1e9).toFixed(1)),
      networkRxMb: parseFloat(((net[0]?.rx_bytes || 0) / 1e6).toFixed(2)),
      networkTxMb: parseFloat(((net[0]?.tx_bytes || 0) / 1e6).toFixed(2)),
      osInfo: `${osInfo.distro} ${osInfo.release}`,
      dockerVersion: `HostPanel v3.0 (Termux-native)`,
      uptimeSeconds: Math.floor(time.uptime),
      activeContainers: activeCount
    });
  } catch (err) {
    console.error('Metrics error:', err);
    res.status(500).json({ error: 'Failed to fetch metrics', details: err.message });
  }
});

module.exports = router;
