const snmp = require('net-snmp');

const OIDS = {
	sysDescr: '1.3.6.1.2.1.1.1.0',
	ssCpuIdle: '1.3.6.1.4.1.2021.11.11.0',
	memTotalReal: '1.3.6.1.4.1.2021.4.5.0',
	memAvailReal: '1.3.6.1.4.1.2021.4.6.0',
	hrProcessorLoad: '1.3.6.1.2.1.25.3.3.1.2',
};

function snmpGet(host, community, oids, timeoutMs = 1500) {
	return new Promise((resolve, reject) => {
		const session = snmp.createSession(host, community, {
			timeout: timeoutMs,
			retries: 1,
		});
		session.get(oids, (err, varbinds) => {
			session.close();
			if (err) return reject(err);

			const out = {};
			for (const vb of varbinds) {
				if (snmp.isVarbindError(vb)) continue;
				out[vb.oid] = vb.value;
			}
			resolve(out);
		});
	});
}

function snmpWalk(host, community, rootOid, timeoutMs = 1500) {
	return new Promise((resolve, reject) => {
		const session = snmp.createSession(host, community, {
			timeout: timeoutMs,
			retries: 1,
		});
		const values = [];
		session.subtree(
			rootOid,
			(varbind) => {
				if (!snmp.isVarbindError(varbind)) values.push(Number(varbind.value));
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

	const got = await snmpGet(host, community, [
		OIDS.sysDescr,
		OIDS.ssCpuIdle,
		OIDS.memTotalReal,
		OIDS.memAvailReal,
	]);

	const sysDescr = String(got[OIDS.sysDescr] ?? '');
	const cpuIdle = Number(got[OIDS.ssCpuIdle] ?? NaN);
	const memTotal = Number(got[OIDS.memTotalReal] ?? NaN);
	const memAvail = Number(got[OIDS.memAvailReal] ?? NaN);

	const cpuUsagePct = Number.isFinite(cpuIdle)
		? Math.max(0, Math.min(100, 100 - cpuIdle))
		: null;

	let ramUsagePct = null;
	if (Number.isFinite(memTotal) && memTotal > 0 && Number.isFinite(memAvail)) {
		ramUsagePct = ((memTotal - memAvail) / memTotal) * 100;
	}

	let hrCpuAvgPct = null;
	try {
		const perCore = await snmpWalk(host, community, OIDS.hrProcessorLoad);
		if (perCore.length)
			hrCpuAvgPct = perCore.reduce((a, b) => a + b, 0) / perCore.length;
	} catch {
	}

	return {
		host,
		ts: new Date(),
		osName: sysDescr,
		cpuUsagePct,
		hrCpuAvgPct,
		ramUsagePct,
		raw: { cpuIdle, memTotal, memAvail },
	};
}

module.exports = { collectNodeMetrics };
