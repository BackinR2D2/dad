import { useState } from 'react';
import { Sidebar, type Page } from './components/app/Sidebar';
import { Topbar } from './components/app/Topbar';
import { ZoomStudio } from './pages/ZoomStudio';
import { Monitoring } from './pages/Monitoring';
import { Toaster } from 'sonner';

export default function App() {
	const [page, setPage] = useState<Page>('zoom');
	const [wsState, setWsState] = useState<
		'connected' | 'disconnected' | 'connecting'
	>('connecting');

	return (
		<div className='min-h-screen bg-zinc-50 text-zinc-950 dark:bg-zinc-950 dark:text-zinc-50'>
			<Toaster richColors position='top-right' />

			<div className='mx-auto grid max-w-7xl grid-cols-12 gap-6 p-6'>
				<aside className='col-span-12 md:col-span-3'>
					<Sidebar page={page} onNavigate={setPage} />
				</aside>

				<main className='col-span-12 md:col-span-9 space-y-6'>
					<Topbar wsState={wsState} />
					{page === 'zoom' ? (
						<ZoomStudio onWsState={setWsState} />
					) : (
						<Monitoring />
					)}
				</main>
			</div>
		</div>
	);
}
