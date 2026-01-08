const snmp = require('net-snmp');

const OIDS = {
	sysDescr: '1.3.6.1.2.1.1.1.0',
	// UCD-SNMP
	ssCpuIdle: '1.3.6.1.4.1.2021.11.11.0',
	memTotalReal: '1.3.6.1.4.1.2021.4.5.0',
	memAvailReal: '1.3.6.1.4.1.2021.4.6.0',
	// Standard-ish HOST-RESOURCES-MIB (per core)
	hrProcessorLoadRoot: '1.3.6.1.2.1.25.3.3.1.2',
};

function createSession(host, community, timeoutMs) {
	return snmp.createSession(host, community, {
		version: snmp.Version2c,
		timeout: timeoutMs,
		retries: 1,
	});
}

function snmpGet(host, community, oids, timeoutMs = 1500) {
	return new Promise((resolve, reject) => {
		const session = createSession(host, community, timeoutMs);

		session.get(oids, (err, varbinds) => {
			session.close();

			if (err) return reject(err);

			const out = {};
			for (const vb of varbinds) {
				if (snmp.isVarbindError(vb)) {
					out[vb.oid] = null;
				} else {
					out[vb.oid] = vb.value;
				}
			}
			resolve(out);
		});
	});
}

// WALK/SUBTREE pentru hrProcessorLoad (per core)
function snmpWalkNumbers(host, community, rootOid, timeoutMs = 1500) {
	return new Promise((resolve, reject) => {
		const session = createSession(host, community, timeoutMs);

		const values = [];
		session.subtree(
			rootOid,
			(vb) => {
				if (!snmp.isVarbindError(vb)) {
					const n = Number(vb.value);
					if (Number.isFinite(n)) values.push(n);
				}
			},
			(err) => {
				session.close();
				if (err) return reject(err);
				resolve(values);
			}
		);
	});
}

async function collectNodeMetrics(node) {
	const { host, community = 'public' } = node;

	const got = await snmpGet(
		host,
		community,
		[OIDS.sysDescr, OIDS.ssCpuIdle, OIDS.memTotalReal, OIDS.memAvailReal],
		2000
	);

	const sysDescr = String(got[OIDS.sysDescr] ?? '');
	const memTotal = Number(got[OIDS.memTotalReal] ?? NaN);
	const memAvail = Number(got[OIDS.memAvailReal] ?? NaN);

	let hrCpuAvgPct = null;
	try {
		const perCore = await snmpWalkNumbers(
			host,
			community,
			OIDS.hrProcessorLoadRoot,
			2000
		);
		if (perCore.length) {
			hrCpuAvgPct = perCore.reduce((a, b) => a + b, 0) / perCore.length;
		}
	} catch {
		// ignore => fallback pe ssCpuIdle
	}

	const cpuIdleRaw = got[OIDS.ssCpuIdle];
	const cpuIdle = cpuIdleRaw == null ? NaN : Number(cpuIdleRaw);

	const cpuUsagePct = Number.isFinite(hrCpuAvgPct)
		? Math.round(hrCpuAvgPct)
		: Number.isFinite(cpuIdle)
		? Math.max(0, Math.min(100, 100 - cpuIdle))
		: null;

	// 3) RAM usage
	let ramUsagePct = null;
	if (Number.isFinite(memTotal) && memTotal > 0 && Number.isFinite(memAvail)) {
		ramUsagePct = ((memTotal - memAvail) / memTotal) * 100;
	}

	return {
		host,
		ts: new Date(),
		osName: sysDescr,
		cpuUsagePct,
		hrCpuAvgPct,
		ramUsagePct,
		raw: {
			cpuIdle: Number.isFinite(cpuIdle) ? cpuIdle : null,
			memTotal: Number.isFinite(memTotal) ? memTotal : null,
			memAvail: Number.isFinite(memAvail) ? memAvail : null,
		},
	};
}

module.exports = { collectNodeMetrics };
