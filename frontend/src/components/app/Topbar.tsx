import { Badge } from '../ui/badge';
import { ThemeMenu } from '../app/ThemeMenu';
import { Wifi, WifiOff } from 'lucide-react';

export function Topbar({
	wsState,
}: {
	wsState: 'connected' | 'disconnected' | 'connecting';
}) {
	return (
		<div className='flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between'>
			<div>
				<div className='text-2xl font-semibold tracking-tight'>
					Control Center
				</div>
				<div className='text-sm text-zinc-500 dark:text-zinc-400'>
					Upload BMP • Live job updates • Download result • Monitor SNMP
				</div>
			</div>

			<div className='flex items-center gap-3'>
				<Badge className='flex items-center gap-1'>
					{wsState === 'connected' ? <Wifi size={14} /> : <WifiOff size={14} />}
					WS: {wsState}
				</Badge>
				<ThemeMenu />
			</div>
		</div>
	);
}
