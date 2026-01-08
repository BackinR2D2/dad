import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from '../ui/card';
import { Separator } from '..//ui/separator';
import { Activity, Image as ImageIcon } from 'lucide-react';

export type Page = 'zoom' | 'monitoring';

export function Sidebar({
	page,
	onNavigate,
}: {
	page: Page;
	onNavigate: (p: Page) => void;
}) {
	return (
		<Card className='sticky top-6'>
			<CardHeader>
				<CardTitle className='flex items-center gap-2'>
					<span className='text-xl'>DAD ZoomOps</span>
					<Badge className='ml-auto'>v1</Badge>
				</CardTitle>
				<CardDescription>
					JMS • MDB • RMI • SNMP • MySQL • MongoDB
				</CardDescription>
			</CardHeader>

			<CardContent className='space-y-2'>
				<Button
					variant={page === 'zoom' ? 'secondary' : 'ghost'}
					className='w-full justify-start gap-2'
					onClick={() => onNavigate('zoom')}
				>
					<ImageIcon size={18} />
					Zoom Studio
				</Button>

				<Button
					variant={page === 'monitoring' ? 'secondary' : 'ghost'}
					className='w-full justify-start gap-2'
					onClick={() => onNavigate('monitoring')}
				>
					<Activity size={18} />
					Monitoring
				</Button>

				<Separator className='my-4' />

				<div className='text-xs text-zinc-500 dark:text-zinc-400'>
					Containers:
					<div className='mt-2 flex flex-wrap gap-2'>
						<Badge>C01</Badge>
						<Badge>C02</Badge>
						<Badge>C03</Badge>
						<Badge>C04</Badge>
						<Badge>C05</Badge>
						<Badge>C06</Badge>
					</div>
				</div>
			</CardContent>
		</Card>
	);
}
