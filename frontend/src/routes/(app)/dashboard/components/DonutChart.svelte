<script lang="ts">
	import { onMount } from 'svelte';
	import { tick } from 'svelte';
	import type { StatusSlice } from '$lib/types/dashboard';

	interface Props {
		slices: StatusSlice[];
		centerValue: string;
		centerLabel: string;
		emptyText?: string;
		ariaLabel: string;
		class?: string;
	}

	let { slices, centerValue, centerLabel, emptyText = '', ariaLabel, class: className = '' }: Props =
		$props();

	const COLOR_HEX: Record<string, string> = {
		brand: '#1c80de',
		warn: '#f59e0b',
		success: '#0ea641',
		danger: '#f2060a',
		muted: '#8b95a7',
		neutral: '#4b5563',
		gray: '#4b5563'
	};

	const COLOR_CLASS: Record<string, string> = {
		brand: 'bg-brand',
		warn: 'bg-warn',
		success: 'bg-success',
		danger: 'bg-danger',
		muted: 'bg-muted',
		neutral: 'bg-gray-600',
		gray: 'bg-gray-600'
	};

	const total = $derived(slices.reduce((sum, slice) => sum + slice.count, 0));

	const segments = $derived.by(() => {
		let cumulative = 0;
		return slices.map((slice) => {
			const pct = total > 0 ? (slice.count / total) * 100 : 0;
			const segment = {
				...slice,
				pct,
				dash: `${pct} ${100 - pct}`,
				offset: -cumulative
			};
			cumulative += pct;
			return segment;
		});
	});

	let animate = $state(false);

	const hex = (color: string) => COLOR_HEX[color] ?? '#8b95a7';
	const swatch = (color: string) => COLOR_CLASS[color] ?? 'bg-muted';

	onMount(async () => {
		await tick();
		animate = true;
	});
</script>

<div class="flex items-center gap-6 {className}">
	<div class="relative shrink-0">
		<svg viewBox="0 0 42 42" class="h-32 w-32 -rotate-90" role="img" aria-label={ariaLabel}>
			<circle
				cx="21"
				cy="21"
				r="15.9"
				fill="transparent"
				stroke="#1a2235"
				stroke-width="5"
			/>
			{#each segments as segment (segment.status)}
				<circle
					data-testid="donut-slice"
					cx="21"
					cy="21"
					r="15.9"
					fill="transparent"
					stroke={hex(segment.color)}
					stroke-width="5"
					stroke-linecap="butt"
					stroke-dasharray={segment.dash}
					stroke-dashoffset={animate ? segment.offset : -100}
					style="transition: stroke-dashoffset 300ms ease"
				/>
			{/each}
		</svg>
		<div class="absolute inset-0 flex flex-col items-center justify-center">
			<span class="text-2xl font-semibold tracking-tight">{centerValue}</span>
			<span class="text-[10px] uppercase tracking-wide text-muted">{centerLabel}</span>
		</div>
	</div>

	{#if total === 0}
		<p class="text-xs text-muted">{emptyText}</p>
	{:else}
		<ul class="flex-1 space-y-2 text-xs">
			{#each segments as segment (segment.status)}
				<li class="flex items-center gap-2">
					<span class="h-2.5 w-2.5 shrink-0 rounded-sm {swatch(segment.color)}"></span>
					<span class="text-ink">{segment.label}</span>
					<span class="ml-auto text-muted">{segment.count}</span>
				</li>
			{/each}
		</ul>
	{/if}
</div>