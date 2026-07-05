const jwt = require('jsonwebtoken');

/**
 * Express middleware: validates Bearer token from Authorization header.
 * Supports both raw API key (matches JWT_SECRET) and signed JWT tokens.
 */
module.exports = function auth(req, res, next) {
  const authHeader = req.headers['authorization'];
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Missing or invalid Authorization header' });
  }
  const token = authHeader.split(' ')[1];
  if (!verifyToken(token)) {
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
  next();
};

/**
 * Standalone token verifier — used by WebSocket routes (logs, terminal)
 * which cannot use Express middleware directly.
 * @param {string} token
 * @returns {boolean}
 */
function verifyToken(token) {
  if (!token) return false;
  try {
    // Simple API-key mode: token matches JWT_SECRET directly
    if (token === (process.env.JWT_SECRET || 'local-phone-key')) return true;
    // Otherwise verify as a signed JWT
    jwt.verify(token, process.env.JWT_SECRET || 'local-phone-key');
    return true;
  } catch {
    return false;
  }
}

module.exports.verifyToken = verifyToken;
