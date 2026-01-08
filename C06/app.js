const express = require('express');
const { MongoClient } = require('mongodb');
const mysql = require('mysql2/promise');
const { collectNodeMetrics } = require('./snmpCollector');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = Number(process.env.PORT || 3000);

const MONGO_URL = process.env.MONGO_URL || 'mongodb://127.0.0.1:27017';
const MONGO_DB = process.env.MONGO_DB || 'dad_metrics';

const MYSQL_HOST = process.env.MYSQL_HOST || '127.0.0.1';
const MYSQL_DB = process.env.MYSQL_DB || 'dad';
const MYSQL_USER = process.env.MYSQL_USER || 'dad';
const MYSQL_PASSWORD = process.env.MYSQL_PASSWORD || 'dad';

const SNMP_COMMUNITY = process.env.SNMP_COMMUNITY || 'public';

const NODES = [
	{ name: 'c01', host: 'c01', community: SNMP_COMMUNITY },
	{ name: 'c02', host: 'c02', community: SNMP_COMMUNITY },
	{ name: 'c03', host: 'c03', community: SNMP_COMMUNITY },
	{ name: 'c04', host: 'c04', community: SNMP_COMMUNITY },
	{ name: 'c05', host: 'c05', community: SNMP_COMMUNITY },
	{ name: 'c06', host: 'c06', community: SNMP_COMMUNITY },
];

let metricsCol;
let mysqlConn;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function retry(label, fn, { tries = 30, delayMs = 2000 } = {}) {
	let lastErr;
	for (let i = 1; i <= tries; i++) {
		try {
			return await fn();
		} catch (e) {
			lastErr = e;
			console.log(
				`[${label}] attempt ${i}/${tries} failed: ${e?.message || e}`
			);
			await sleep(delayMs);
		}
	}
	throw lastErr;
}

async function initMongo() {
	const client = await retry('mongo-connect', async () => {
		const c = new MongoClient(MONGO_URL);
		await c.connect();
		return c;
	});

	const db = client.db(MONGO_DB);
	metricsCol = db.collection('snmp_metrics');
	await metricsCol.createIndex({ host: 1, ts: -1 });
}

async function initMySQL() {
	mysqlConn = await retry('mysql-connect', async () => {
		const conn = await mysql.createConnection({
			host: MYSQL_HOST,
			user: MYSQL_USER,
			password: MYSQL_PASSWORD,
			database: MYSQL_DB,
		});
		await conn.query('SELECT 1');
		return conn;
	});

	await mysqlConn.execute(`
    CREATE TABLE IF NOT EXISTS images (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      filename VARCHAR(255),
      mime VARCHAR(100) DEFAULT 'image/bmp',
      data LONGBLOB NOT NULL
    )
  `);
}

async function pollAll() {
	const results = [];
	for (const n of NODES) {
		try {
			const m = await collectNodeMetrics(n);
			results.push({ ...m, nodeName: n.name });
		} catch (e) {
			results.push({
				host: n.host,
				nodeName: n.name,
				ts: new Date(),
				error: String(e?.message || e),
			});
		}
	}
	if (results.length) await metricsCol.insertMany(results);
}

// --- SNMP endpoints ---
app.get('/snmp/latest', async (req, res) => {
	const out = {};
	for (const n of NODES) {
		const doc = await metricsCol
			.find({ host: n.host })
			.sort({ ts: -1 })
			.limit(1)
			.next();
		out[n.name] = doc || null;
	}
	res.json(out);
});

app.get('/snmp/history', async (req, res) => {
	const node = String(req.query.node || '');
	const limit = Math.min(200, Math.max(1, Number(req.query.limit || 50)));
	const found = NODES.find((x) => x.name === node);
	if (!found)
		return res.status(400).json({ error: 'Unknown node. Use node=c01..c05' });

	const docs = await metricsCol
		.find({ host: found.host })
		.sort({ ts: -1 })
		.limit(limit)
		.toArray();
	res.json({ node, items: docs });
});

app.post('/snmp/poll', async (req, res) => {
	await pollAll();
	res.json({ ok: true });
});

// --- Image endpoints ---
app.post(
	'/images',
	express.raw({ type: '*/*', limit: '200mb' }),
	async (req, res) => {
		try {
			const zoomIn = String(req.query.zoomIn || 'true') === 'true';
			const percent = Number(req.query.percent || 0);
			const filename = String(req.query.filename || 'result.bmp');
			const mime = String(req.query.mime || 'image/bmp');

			const data = req.body;
			if (!data || !data.length)
				return res.status(400).json({ error: 'Empty body' });

			const [result] = await mysqlConn.execute(
				'INSERT INTO images(filename, mime, data) VALUES(?, ?, ?)',
				[filename, mime, data]
			);
			console.log('DEBUG IMAGE POST');
			res.json({ id: result.insertId, zoomIn, percent, filename });
		} catch (e) {
			res.status(500).json({ error: String(e?.message || e) });
		}
	}
);

app.get('/images/:id', async (req, res) => {
	const id = Number(req.params.id);
	const [rows] = await mysqlConn.execute(
		'SELECT id, filename, mime, data FROM images WHERE id = ?',
		[id]
	);
	if (!rows.length) return res.status(404).json({ error: 'Not found' });

	const row = rows[0];
	res.setHeader('Content-Type', row.mime || 'application/octet-stream');
	res.setHeader(
		'Content-Disposition',
		`attachment; filename="${row.filename || `image_${row.id}.bmp`}"`
	);
	res.send(row.data);
});

app.get('/health', (req, res) => res.json({ ok: true }));

(async () => {
	await initMongo();
	await initMySQL();

	await pollAll();
	setInterval(pollAll, 5000);

	app.listen(PORT, '0.0.0.0', () => console.log('C06 API listening on', PORT));
})();
