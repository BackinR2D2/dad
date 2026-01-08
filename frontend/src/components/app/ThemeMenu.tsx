import { Button } from '../ui/button';
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuTrigger,
} from '../ui/dropdown-menu';
import { Moon, Sun, Laptop } from 'lucide-react';
import { applyTheme, getStoredTheme, type ThemeMode } from '../../lib/theme';
import { useState } from 'react';

export function ThemeMenu() {
	const [mode, setMode] = useState<ThemeMode>(() => getStoredTheme());

	function setTheme(m: ThemeMode) {
		setMode(m);
		applyTheme(m);
	}

	return (
		<DropdownMenu>
			<DropdownMenuTrigger asChild>
				<Button variant='outline' className='gap-2'>
					{mode === 'dark' ? (
						<Moon size={16} />
					) : mode === 'light' ? (
						<Sun size={16} />
					) : (
						<Laptop size={16} />
					)}
					Theme
				</Button>
			</DropdownMenuTrigger>
			<DropdownMenuContent align='end' className='rounded-xl'>
				<DropdownMenuItem onClick={() => setTheme('light')} className='gap-2'>
					<Sun size={16} /> Light
				</DropdownMenuItem>
				<DropdownMenuItem onClick={() => setTheme('dark')} className='gap-2'>
					<Moon size={16} /> Dark
				</DropdownMenuItem>
				<DropdownMenuItem onClick={() => setTheme('system')} className='gap-2'>
					<Laptop size={16} /> System
				</DropdownMenuItem>
			</DropdownMenuContent>
		</DropdownMenu>
	);
}
