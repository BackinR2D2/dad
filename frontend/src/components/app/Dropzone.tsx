import { useCallback, useMemo, useState } from 'react';
import { cn } from '../../lib/utils';
import { UploadCloud } from 'lucide-react';

type Props = {
	file: File | null;
	onFile: (f: File | null) => void;
	disabled?: boolean;
};

export function Dropzone({ file, onFile, disabled }: Props) {
	const [drag, setDrag] = useState(false);

	const hint = useMemo(() => {
		if (!file) return 'Drop BMP here, or click to browse';
		return 'Click to replace file';
	}, [file]);

	const onDrop = useCallback(
		(e: React.DragEvent<HTMLDivElement>) => {
			e.preventDefault();
			if (disabled) return;
			setDrag(false);

			const f = e.dataTransfer.files?.[0];
			if (!f) return;
			onFile(f);
		},
		[disabled, onFile]
	);

	return (
		<div
			className={cn(
				'group relative flex min-h-[190px] cursor-pointer flex-col items-center justify-center rounded-2xl border border-dashed p-6 text-center transition',
				drag
					? 'border-zinc-400 bg-zinc-100 dark:bg-zinc-900'
					: 'border-zinc-300 bg-white hover:bg-zinc-50 dark:border-zinc-800 dark:bg-zinc-950 dark:hover:bg-zinc-900',
				disabled && 'cursor-not-allowed opacity-70'
			)}
			onClick={() =>
				!disabled && document.getElementById('file-input')?.click()
			}
			onDragOver={(e) => {
				e.preventDefault();
				if (!disabled) setDrag(true);
			}}
			onDragLeave={() => setDrag(false)}
			onDrop={onDrop}
		>
			<input
				id='file-input'
				type='file'
				accept='.bmp,image/bmp'
				className='hidden'
				disabled={disabled}
				onChange={(e) => onFile(e.target.files?.[0] || null)}
			/>

			<UploadCloud className='mb-3 opacity-70' />
			<div className='text-sm font-medium'>{hint}</div>
			<div className='mt-1 text-xs text-zinc-500 dark:text-zinc-400'>
				Recommended: 24/32-bit uncompressed BMP (BI_RGB)
			</div>

			{file && (
				<div className='mt-4 w-full rounded-xl border border-zinc-200 bg-zinc-50 p-3 text-left text-xs dark:border-zinc-800 dark:bg-zinc-900'>
					<div className='font-semibold'>{file.name}</div>
					<div className='text-zinc-600 dark:text-zinc-300'>
						{(file.size / (1024 * 1024)).toFixed(2)} MB
					</div>
				</div>
			)}
		</div>
	);
}
