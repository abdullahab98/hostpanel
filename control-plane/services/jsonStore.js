/**
 * services/jsonStore.js
 * Zero-dependency JSON-file-based storage.
 * Drop-in replacement for better-sqlite3 on Android/Termux.
 * 
 * API mirrors better-sqlite3:
 *   const db = new JsonStore(path)
 *   db.exec(sql)              — runs CREATE TABLE statements (no-op, schema inferred)
 *   db.prepare(sql).run(...)  — INSERT / UPDATE / DELETE
 *   db.prepare(sql).all(...)  — SELECT (returns array)
 *   db.prepare(sql).get(...)  — SELECT (returns first row or undefined)
 *   db.close()                — flush to disk
 */

const fs = require('fs');
const path = require('path');

class JsonStore {
  constructor(dbPath) {
    this._path = dbPath;
    this._tables = {};
    this._load();
  }

  _load() {
    const jsonPath = this._path.replace(/\.db$/, '.json');
    this._jsonPath = jsonPath;
    if (fs.existsSync(jsonPath)) {
      try {
        this._tables = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
      } catch {
        this._tables = {};
      }
    }
  }

  _save() {
    const dir = path.dirname(this._jsonPath);
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(this._jsonPath, JSON.stringify(this._tables, null, 2));
  }

  _tableFromSql(sql) {
    const m = sql.match(/(?:FROM|INTO|UPDATE|TABLE(?:\s+IF\s+NOT\s+EXISTS)?)\s+["'`]?(\w+)["'`]?/i);
    return m ? m[1] : null;
  }

  // Ensure table array exists
  _ensure(table) {
    if (!this._tables[table]) this._tables[table] = [];
  }

  exec(sql) {
    // Handle CREATE TABLE — just ensure the table exists
    const creates = [...sql.matchAll(/CREATE TABLE(?:\s+IF\s+NOT\s+EXISTS)?\s+["'`]?(\w+)["'`]?/gi)];
    for (const m of creates) {
      this._ensure(m[1]);
    }
    const indexes = [...sql.matchAll(/CREATE INDEX[^;]*/gi)];
    // Indexes are no-ops for JSON storage
    this._save();
    return this;
  }

  prepare(sql) {
    const self = this;
    const trimmed = sql.trim().toUpperCase();

    return {
      run(...params) {
        const table = self._tableFromSql(sql);
        if (!table) return { changes: 0 };
        self._ensure(table);

        if (trimmed.startsWith('INSERT')) {
          const row = self._parseInsert(sql, params, table);
          // INSERT OR REPLACE
          if (trimmed.includes('OR REPLACE') && row.name !== undefined) {
            const idx = self._tables[table].findIndex(r => r.name === row.name);
            if (idx >= 0) self._tables[table][idx] = { ...self._tables[table][idx], ...row };
            else self._tables[table].push(row);
          } else {
            self._tables[table].push(row);
          }
        } else if (trimmed.startsWith('UPDATE')) {
          self._applyUpdate(sql, params, table);
        } else if (trimmed.startsWith('DELETE')) {
          self._applyDelete(sql, params, table);
        }

        self._save();
        return { changes: 1 };
      },

      all(...params) {
        const table = self._tableFromSql(sql);
        if (!table) return [];
        self._ensure(table);
        return self._applySelect(sql, params, table);
      },

      get(...params) {
        const table = self._tableFromSql(sql);
        if (!table) return undefined;
        self._ensure(table);
        const rows = self._applySelect(sql, params, table);
        return rows[0];
      }
    };
  }

  _parseInsert(sql, params, table) {
    // Extract column names from INSERT INTO table (col1, col2...) VALUES (?,?...)
    const colMatch = sql.match(/\(([^)]+)\)\s*VALUES/i);
    if (!colMatch) return {};
    const cols = colMatch[1].split(',').map(c => c.trim().replace(/["`']/g, ''));
    const row = {};
    cols.forEach((col, i) => {
      row[col] = params[i] !== undefined ? params[i] : null;
    });
    // Default created_at if not provided
    if (cols.includes('created_at') && row.created_at === null) {
      row.created_at = new Date().toISOString().replace('T', ' ').slice(0, 19);
    }
    return row;
  }

  _applyUpdate(sql, params, table) {
    // Parse: UPDATE table SET col1=?, col2=? WHERE col=?
    const setMatch = sql.match(/SET\s+(.+?)\s+WHERE\s+(.+)/i);
    if (!setMatch) return;

    const setParts = setMatch[1].split(',').map(s => s.trim());
    const wherePart = setMatch[2].trim();

    const setCols = setParts.map(p => {
      const [col] = p.split('=');
      return col.trim().replace(/["`']/g, '');
    });

    const whereCol = wherePart.split('=')[0].trim().replace(/["`']/g, '');

    // Last param is the WHERE value, rest are SET values
    const setValues = params.slice(0, setCols.length);
    const whereValue = params[setCols.length];

    this._tables[table] = this._tables[table].map(row => {
      if (String(row[whereCol]) === String(whereValue)) {
        const updated = { ...row };
        setCols.forEach((col, i) => {
          // Handle SQL expressions like "status='running'"
          if (setValues[i] !== undefined) {
            updated[col] = setValues[i];
          } else {
            // Extract literal value from SET clause like col='value'
            const part = setParts[i];
            const litMatch = part.match(/=\s*'([^']*)'/) || part.match(/=\s*(\w+)\(.*\)/);
            if (litMatch) {
              if (part.includes("datetime('now')")) {
                updated[col] = new Date().toISOString().replace('T', ' ').slice(0, 19);
              } else {
                updated[col] = litMatch[1];
              }
            } else {
              updated[col] = null;
            }
          }
        });
        return updated;
      }
      return row;
    });
  }

  _applyDelete(sql, params, table) {
    const whereMatch = sql.match(/WHERE\s+(.+)/i);
    if (!whereMatch) {
      this._tables[table] = [];
      return;
    }
    const wherePart = whereMatch[1].trim();
    const whereCol = wherePart.split('=')[0].trim().replace(/["`']/g, '');
    const whereValue = params[0];
    this._tables[table] = this._tables[table].filter(
      row => String(row[whereCol]) !== String(whereValue)
    );
  }

  _applySelect(sql, params, table) {
    let rows = [...(this._tables[table] || [])];

    // WHERE clause
    const whereMatch = sql.match(/WHERE\s+(.+?)(?:\s+ORDER|\s+LIMIT|$)/i);
    if (whereMatch && params.length > 0) {
      const wherePart = whereMatch[1].trim();
      const conditions = wherePart.split(/\s+AND\s+/i);
      let paramIdx = 0;
      for (const cond of conditions) {
        if (cond.includes('IS NOT NULL')) {
          const col = cond.split('IS')[0].trim().replace(/["`']/g, '');
          rows = rows.filter(r => r[col] != null);
        } else if (cond.includes('IS NULL')) {
          const col = cond.split('IS')[0].trim().replace(/["`']/g, '');
          rows = rows.filter(r => r[col] == null);
        } else if (cond.includes('=') && paramIdx < params.length) {
          const col = cond.split('=')[0].trim().replace(/["`']/g, '');
          const val = params[paramIdx++];
          rows = rows.filter(r => String(r[col]) === String(val));
        }
      }
    }

    // ORDER BY
    const orderMatch = sql.match(/ORDER\s+BY\s+(\w+)(?:\s+(DESC|ASC))?/i);
    if (orderMatch) {
      const col = orderMatch[1];
      const desc = orderMatch[2] && orderMatch[2].toUpperCase() === 'DESC';
      rows.sort((a, b) => {
        if (a[col] < b[col]) return desc ? 1 : -1;
        if (a[col] > b[col]) return desc ? -1 : 1;
        return 0;
      });
    }

    // LIMIT
    const limitMatch = sql.match(/LIMIT\s+(\d+|\?)/i);
    if (limitMatch) {
      const lim = limitMatch[1] === '?' ? params[params.length - 1] : parseInt(limitMatch[1]);
      rows = rows.slice(0, lim);
    }

    return rows;
  }

  close() {
    this._save();
  }
}

module.exports = JsonStore;
