import { useEffect, useRef, useState } from 'react';
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Badge } from '../components/ui/badge';
import { toast } from 'sonner';
import { createJob, getJob, type JobStatusResponse } from '../lib/api';
import { Dropzone } from '../components/app/Dropzone';
import { Download } from 'lucide-react';

const C01_WS = import.meta.env.VITE_C01_WS as string;

export function ZoomStudio({ onWsState }: { onWsState: (s: any) => void }) {
	const [file, setFile] = useState<File | null>(null);
	const [zoomIn, setZoomIn] = useState(true);
	const [percent, setPercent] = useState(20);

	const [jobId, setJobId] = useState<string | null>(null);
	const [status, setStatus] = useState<JobStatusResponse | null>(null);
	const [busy, setBusy] = useState(false);

	const wsRef = useRef<WebSocket | null>(null);

	// WS connect (once)
	useEffect(() => {
		onWsState('connecting');
		const ws = new WebSocket(C01_WS);
		wsRef.current = ws;

		ws.onopen = () => onWsState('connected');
		ws.onclose = () => onWsState('disconnected');
		ws.onerror = () => onWsState('disconnected');

		ws.onmessage = (ev) => {
			try {
				const msg = JSON.parse(ev.data);
				if (msg?.status === 'DONE' && typeof msg?.jobId === 'string') {
					const imgId =
						msg.downloadUrl?.split('/')[
							msg.downloadUrl?.split('/')?.length - 1
						];
					setStatus({
						jobId: msg.jobId,
						status: 'DONE',
						imageId: msg.imageId,
						downloadUrl: `http://localhost:3000/images/${imgId}`,
					});
					toast.success('Job finished', {
						description: `Image ready: #${msg.imageId}`,
					});
				}
			} catch {}
		};

		return () => {
			try {
				ws.close();
			} catch {}
		};
	}, [onWsState]);

	// Polling fallback while PENDING
	useEffect(() => {
		if (!jobId) return;
		if (!status || status.status !== 'PENDING') return;

		const t = setInterval(async () => {
			try {
				const st = await getJob(jobId);
				setStatus(st);
				if (st.status === 'DONE')
					toast.success('Job finished', {
						description: `Image ready: #${st.imageId}`,
					});
			} catch {}
		}, 1000);

		return () => clearInterval(t);
	}, [jobId, status]);

	async function submit() {
		if (!file) {
			toast.error('Choose a BMP first');
			return;
		}
		setBusy(true);
		setStatus(null);
		setJobId(null);

		try {
			const resp = await createJob(file, zoomIn, percent);
			setJobId(resp.jobId);
			setStatus({ jobId: resp.jobId, status: 'PENDING' });
			toast.message('Job submitted', { description: `jobId: ${resp.jobId}` });
		} catch (e: any) {
			toast.error('Upload failed', { description: String(e?.message || e) });
		} finally {
			setBusy(false);
		}
	}

	function download() {
		if (!status || status.status !== 'DONE') return;
		window.open(status.downloadUrl, '_blank', 'noopener,noreferrer');
	}

	return (
		<div className='grid grid-cols-1 gap-6 lg:grid-cols-5'>
			<Card className='lg:col-span-3'>
				<CardHeader>
					<CardTitle>Zoom Studio</CardTitle>
					<CardDescription>
						Upload a BMP and process it through your 6-container architecture.
					</CardDescription>
				</CardHeader>

				<CardContent className='space-y-5'>
					<Dropzone file={file} onFile={setFile} disabled={busy} />

					<div className='grid grid-cols-1 gap-4'>
						<div className='space-y-2'>
							<Label>Mode</Label>
							<div className='flex gap-2'>
								<Button
									type='button'
									variant={zoomIn ? 'secondary' : 'outline'}
									className='flex-1'
									onClick={() => setZoomIn(true)}
									disabled={busy}
								>
									Zoom In
								</Button>
								<Button
									type='button'
									variant={!zoomIn ? 'secondary' : 'outline'}
									className='flex-1'
									onClick={() => setZoomIn(false)}
									disabled={busy}
								>
									Zoom Out
								</Button>
							</div>
						</div>

						<div className='space-y-2'>
							<Label>Percent</Label>
							<Input
								type='number'
								min={1}
								max={90}
								value={percent}
								disabled={busy}
								onChange={(e) => setPercent(Number(e.target.value))}
							/>
							<div className='text-xs text-zinc-500'>Recommended: 10–40%</div>
						</div>

						<div className='space-y-2'>
							<Label>Action</Label>
							<Button
								onClick={submit}
								disabled={!file || busy}
								className='w-full'
							>
								{busy ? 'Submitting...' : 'Start Job'}
							</Button>

							<Button
								onClick={download}
								disabled={!status || status.status !== 'DONE'}
								variant='outline'
								className='w-full gap-2'
							>
								<Download size={16} /> Download
							</Button>
						</div>
					</div>
				</CardContent>
			</Card>

			<Card className='lg:col-span-2'>
				<CardHeader>
					<CardTitle>Job Status</CardTitle>
					<CardDescription>
						WebSocket live updates + polling fallback.
					</CardDescription>
				</CardHeader>
				<CardContent className='space-y-4'>
					<div className='rounded-2xl border border-zinc-200 p-4 dark:border-zinc-800'>
						<div className='text-xs text-zinc-500'>jobId</div>
						<div className='mt-1 break-all text-sm font-semibold'>
							{jobId ?? '—'}
						</div>
					</div>

					<div className='rounded-2xl border border-zinc-200 p-4 dark:border-zinc-800'>
						<div className='text-xs text-zinc-500'>status</div>
						<div className='mt-1'>
							<Badge className='uppercase'>{status?.status ?? '—'}</Badge>
						</div>

						{status?.status === 'DONE' && (
							<div className='mt-3 text-sm'>
								<div className='text-xs text-zinc-500'>imageId</div>
								<div className='font-semibold'>{status.imageId}</div>
								<div className='mt-2 text-xs text-zinc-500'>downloadUrl</div>
								<a
									className='break-all text-sm underline'
									href={status.downloadUrl}
									target='_blank'
									rel='noreferrer'
								>
									{status.downloadUrl}
								</a>
							</div>
						)}
					</div>
				</CardContent>
			</Card>
		</div>
	);
}
