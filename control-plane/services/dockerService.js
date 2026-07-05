const Docker = require('dockerode');
const net = require('net');
const path = require('path');

const docker = new Docker({ socketPath: process.env.DOCKER_SOCKET || '/var/run/docker.sock' });

async function listContainers() {
  const containers = await docker.listContainers({ all: true });
  const detailed = await Promise.all(containers.map(async (c) => {
    try {
      const container = docker.getContainer(c.Id);
      const stats = await container.stats({ stream: false }).catch(() => null);
      const cpuPercent = stats ? calcCpuPercent(stats) : 0;
      const memMb = stats ? (stats.memory_stats.usage / 1e6) : 0;
      return {
        id: c.Id.substring(0, 12),
        name: c.Names[0]?.replace('/', '') || '',
        image: c.Image,
        status: mapStatus(c.State),
        created: new Date(c.Created * 1000).toLocaleDateString(),
        ports: c.Ports.map(p => `${p.PublicPort || ''}:${p.PrivatePort}`).join(', '),
        customDomain: null,
        framework: detectFramework(c.Image),
        cpuPercent: parseFloat(cpuPercent.toFixed(2)),
        memoryUsageMb: parseFloat(memMb.toFixed(1)),
        gitUrl: null,
        branch: 'main',
        sslEnabled: false,
        uptime: c.Status || ''
      };
    } catch { return null; }
  }));
  return detailed.filter(Boolean);
}

async function getContainer(name) {
  const containers = await docker.listContainers({ all: true, filters: { name: [name] } });
  if (!containers.length) throw new Error(`Container ${name} not found`);
  const c = containers[0];
  return {
    id: c.Id.substring(0, 12),
    name: c.Names[0]?.replace('/', '') || name,
    image: c.Image,
    status: mapStatus(c.State),
    created: new Date(c.Created * 1000).toLocaleDateString(),
    ports: c.Ports.map(p => `${p.PublicPort || '?'}:${p.PrivatePort}`).join(', '),
    framework: detectFramework(c.Image),
    uptime: c.Status || ''
  };
}

async function startContainer(name) {
  const container = docker.getContainer(name);
  await container.start();
}

async function stopContainer(name) {
  const container = docker.getContainer(name);
  await container.stop();
}

async function restartContainer(name) {
  const container = docker.getContainer(name);
  await container.restart();
}

async function rebuildContainer(name) {
  const projectDir = path.join(process.env.PROJECTS_DIR || '/opt/hostpanel/projects', name);
  await buildImage(projectDir, name);
  const info = await getContainer(name).catch(() => null);
  if (info) {
    await stopContainer(name).catch(() => {});
    await docker.getContainer(name).remove().catch(() => {});
  }
}

async function deleteContainer(name) {
  const container = docker.getContainer(name);
  await container.stop().catch(() => {});
  await container.remove({ force: true });
}

async function getEnvVars(name) {
  const container = docker.getContainer(name);
  const info = await container.inspect();
  const envArray = info.Config.Env || [];
  return Object.fromEntries(envArray.map(e => {
    const idx = e.indexOf('=');
    return [e.substring(0, idx), e.substring(idx + 1)];
  }));
}

async function updateEnvVars(name, envVars) {
  // Store env for next rebuild - write to project .env file
  const envPath = path.join(process.env.PROJECTS_DIR || '/opt/hostpanel/projects', name, '.env');
  const content = Object.entries(envVars).map(([k, v]) => `${k}=${v}`).join('\n');
  require('fs').writeFileSync(envPath, content);
}

async function buildImage(projectDir, imageName) {
  return new Promise((resolve, reject) => {
    docker.buildImage({ context: projectDir, src: ['.'] }, { t: `${imageName}:latest` }, (err, stream) => {
      if (err) return reject(err);
      docker.modem.followProgress(stream, (err, output) => {
        if (err) return reject(err);
        const errors = output.filter(o => o.error);
        if (errors.length) return reject(new Error(errors[0].error));
        resolve(output);
      }, (event) => {
        if (event.stream) process.stdout.write(event.stream);
      });
    });
  });
}

async function runContainer(name, imageName, hostPort, containerPort, envVars = {}) {
  const envArray = Object.entries(envVars).map(([k, v]) => `${k}=${v}`);
  const container = await docker.createContainer({
    name,
    Image: `${imageName}:latest`,
    Env: envArray,
    ExposedPorts: { [`${containerPort}/tcp`]: {} },
    HostConfig: {
      PortBindings: { [`${containerPort}/tcp`]: [{ HostPort: `${hostPort}` }] },
      RestartPolicy: { Name: 'unless-stopped' }
    }
  });
  await container.start();
}

async function findFreePort() {
  return new Promise((resolve) => {
    const srv = net.createServer();
    srv.listen(0, () => {
      const port = srv.address().port;
      srv.close(() => resolve(port));
    });
  });
}

function mapStatus(state) {
  const map = { running: 'running', exited: 'stopped', paused: 'stopped', restarting: 'restarting', created: 'stopped' };
  return map[state?.toLowerCase()] || 'stopped';
}

function detectFramework(image) {
  if (image.includes('node')) return 'nodejs';
  if (image.includes('python')) return 'django';
  if (image.includes('maven') || image.includes('java') || image.includes('temurin')) return 'springboot';
  if (image.includes('nginx') || image.includes('alpine')) return 'static';
  return 'nodejs';
}

function calcCpuPercent(stats) {
  try {
    const cpuDelta = stats.cpu_stats.cpu_usage.total_usage - stats.precpu_stats.cpu_usage.total_usage;
    const sysDelta = stats.cpu_stats.system_cpu_usage - stats.precpu_stats.system_cpu_usage;
    const cpuCount = stats.cpu_stats.online_cpus || 1;
    return (cpuDelta / sysDelta) * cpuCount * 100;
  } catch { return 0; }
}

module.exports = { listContainers, getContainer, startContainer, stopContainer, restartContainer, rebuildContainer, deleteContainer, getEnvVars, updateEnvVars, buildImage, runContainer, findFreePort };
