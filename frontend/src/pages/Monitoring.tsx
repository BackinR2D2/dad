import { useEffect, useMemo, useState } from 'react';
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { getSnmpHistory, getSnmpLatest } from '../lib/api';
import { toast } from 'sonner';

const nodes = ['c01', 'c02', 'c03', 'c04', 'c05', 'c06'] as const;

export function Monitoring() {
	const [latest, setLatest] = useState<Record<string, any> | null>(null);
	const [selected, setSelected] = useState<(typeof nodes)[number]>('c01');
	const [history, setHistory] = useState<any[] | null>(null);
	const [loading, setLoading] = useState(false);

	async function refresh() {
		setLoading(true);
		try {
			const l = await getSnmpLatest();
			setLatest(l);
			const h = await getSnmpHistory(selected, 30);
			setHistory(h.items);
		} catch (e: any) {
			toast.error('SNMP fetch failed', {
				description: String(e?.message || e),
			});
		} finally {
			setLoading(false);
		}
	}

	useEffect(() => {
		refresh();
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [selected]);

	const cards = useMemo(() => {
		if (!latest) return [];
		return nodes.map((n) => ({ node: n, data: latest[n] }));
	}, [latest]);

	return (
		<div className='space-y-6'>
			<Card>
				<CardHeader className='flex flex-row items-start justify-between gap-4'>
					<div>
						<CardTitle>SNMP Monitoring</CardTitle>
						<CardDescription>
							Real SNMP polling (C06) → MongoDB → REST.
						</CardDescription>
					</div>
					<Button onClick={refresh} variant='outline' disabled={loading}>
						{loading ? 'Refreshing...' : 'Refresh'}
					</Button>
				</CardHeader>
			</Card>

			<div className='grid grid-cols-1 gap-6 md:grid-cols-2'>
				{cards.map((c) => (
					<Card
						key={c.node}
						className={
							selected === c.node
								? 'ring-2 ring-zinc-300 dark:ring-zinc-700'
								: ''
						}
					>
						<CardHeader className='flex flex-row items-center justify-between'>
							<CardTitle className='text-base'>
								{c.node.toUpperCase()}
							</CardTitle>
							<Button variant='ghost' onClick={() => setSelected(c.node)}>
								View
							</Button>
						</CardHeader>
						<CardContent className='space-y-2 text-sm'>
							{!c.data ? (
								<div className='text-zinc-500'>No data</div>
							) : c.data.error ? (
								<div className='text-red-600'>SNMP error: {c.data.error}</div>
							) : (
								<>
									<div className='text-xs text-zinc-500'>OS</div>
									<div className='line-clamp-2'>
										{String(c.data.osName || '').slice(0, 160)}
									</div>

									<div className='mt-3 flex gap-2'>
										<Badge>
											CPU:{' '}
											{c.data.cpuUsagePct?.toFixed?.(1) ??
												c.data.cpuUsagePct ??
												'—'}
											%
										</Badge>
										<Badge>
											RAM:{' '}
											{c.data.ramUsagePct?.toFixed?.(1) ??
												c.data.ramUsagePct ??
												'—'}
											%
										</Badge>
									</div>

									<div className='mt-3 text-xs text-zinc-500'>
										ts: {c.data.ts ? new Date(c.data.ts).toLocaleString() : '—'}
									</div>
								</>
							)}
						</CardContent>
					</Card>
				))}
			</div>

			<Card>
				<CardHeader>
					<CardTitle>History — {selected.toUpperCase()}</CardTitle>
					<CardDescription>Last 30 samples.</CardDescription>
				</CardHeader>
				<CardContent>
					{!history ? (
						<div className='text-zinc-500'>No history</div>
					) : (
						<div className='overflow-auto rounded-xl border border-zinc-200 dark:border-zinc-800'>
							<table className='w-full text-sm'>
								<thead className='bg-zinc-50 text-left dark:bg-zinc-900'>
									<tr>
										<th className='p-3'>Time</th>
										<th className='p-3'>CPU%</th>
										<th className='p-3'>RAM%</th>
										<th className='p-3'>OS</th>
									</tr>
								</thead>
								<tbody>
									{history.map((row, idx) => (
										<tr
											key={idx}
											className='border-t border-zinc-200 dark:border-zinc-800'
										>
											<td className='p-3 whitespace-nowrap'>
												{row.ts ? new Date(row.ts).toLocaleTimeString() : '—'}
											</td>
											<td className='p-3'>
												{row.cpuUsagePct?.toFixed?.(1) ??
													row.cpuUsagePct ??
													'—'}
											</td>
											<td className='p-3'>
												{row.ramUsagePct?.toFixed?.(1) ??
													row.ramUsagePct ??
													'—'}
											</td>
											<td className='p-3 max-w-[520px] truncate'>
												{row.osName ?? row.error ?? '—'}
											</td>
										</tr>
									))}
								</tbody>
							</table>
						</div>
					)}
				</CardContent>
			</Card>
		</div>
	);
}
