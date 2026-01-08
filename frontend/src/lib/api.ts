const C01_HTTP = import.meta.env.VITE_C01_HTTP as string;
const C06_HTTP = import.meta.env.VITE_C06_HTTP as string;

export type JobCreateResponse = { jobId: string; status: "PENDING" };

export type JobStatusResponse =
  | { jobId: string; status: "PENDING" }
  | { jobId: string; status: "DONE"; imageId: number; downloadUrl: string };

export async function createJob(file: File, zoomIn: boolean, percent: number) {
  const fd = new FormData();
  fd.append("file", file);
  fd.append("zoomIn", String(zoomIn));
  fd.append("percent", String(percent));

  const res = await fetch(`${C01_HTTP}/api/jobs`, { method: "POST", body: fd });
  if (!res.ok) throw new Error(`Upload failed: ${res.status}`);
  return (await res.json()) as JobCreateResponse;
}

export async function getJob(jobId: string) {
  const res = await fetch(`${C01_HTTP}/api/jobs/${jobId}`);
  if (!res.ok) throw new Error(`Status failed: ${res.status}`);
  return (await res.json()) as JobStatusResponse;
}

export async function getSnmpLatest() {
  const res = await fetch(`${C06_HTTP}/snmp/latest`);
  if (!res.ok) throw new Error(`SNMP latest failed: ${res.status}`);
  return (await res.json()) as Record<string, any>;
}

export async function getSnmpHistory(node: string, limit = 30) {
  const res = await fetch(
    `${C06_HTTP}/snmp/history?node=${encodeURIComponent(node)}&limit=${limit}`
  );
  if (!res.ok) throw new Error(`SNMP history failed: ${res.status}`);
  return (await res.json()) as { node: string; items: any[] };
}
