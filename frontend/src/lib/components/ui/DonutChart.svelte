<script lang="ts">
	interface Segment {
		value: number;
		color: string;
		label?: string;
	}

	interface Props {
		total: number;
		segments: Segment[];
		label?: string;
	}

	let { total, segments, label }: Props = $props();

	const TONE_HEX: Record<string, string> = {
		brand: '#1c80de',
		success: '#0ea641',
		warn: '#f59e0b',
		danger: '#f2060a',
		muted: '#8b95a7'
	};

	const soma = $derived(segments.reduce((acc, s) => acc + s.value, 0));

	const fatias = $derived.by(() => {
		let acumulado = 0;
		return segments.map((segment) => {
			const pct = total > 0 ? (segment.value / total) * 100 : 0;
			const fatia = {
				...segment,
				pct,
				dasharray: `${pct} ${100 - pct}`,
				dashoffset: -acumulado
			};
			acumulado += pct;
			return fatia;
		});
	});

	const ariaLabel = $derived(label ?? `Progresso ${soma} de ${total}`);

	function cor(color: string): string {
		if (color.startsWith('#')) return color;
		return TONE_HEX[color] ?? '#8b95a7';
	}
</script>

<div class="flex items-center gap-4">
	<div class="relative shrink-0">
		<svg viewBox="0 0 42 42" class="h-28 w-28 -rotate-90" role="img" aria-label={ariaLabel}>
			<circle cx="21" cy="21" r="15.9" fill="transparent" stroke="#1a2235" stroke-width="5" />
			{#each fatias as fatia, i (i)}
				<circle
					cx="21"
					cy="21"
					r="15.9"
					fill="transparent"
					stroke={cor(fatia.color)}
					stroke-width="5"
					stroke-linecap="butt"
					stroke-dasharray={fatia.dasharray}
					stroke-dashoffset={fatia.dashoffset}
				/>
			{/each}
		</svg>
		<div class="absolute inset-0 flex flex-col items-center justify-center">
			<span class="text-lg font-semibold tracking-tight">{soma}/{total}</span>
			{#if label}
				<span class="text-[10px] uppercase tracking-wide text-muted">{label}</span>
			{/if}
		</div>
	</div>
	{#if segments.some((s) => s.label)}
		<ul class="flex-1 space-y-1.5 text-xs">
			{#each segments as segment, i (i)}
				{#if segment.label}
					<li class="flex items-center gap-2">
						<span
							class="h-2.5 w-2.5 shrink-0 rounded-sm"
							style="background-color: {cor(segment.color)}"
							aria-hidden="true"
						></span>
						<span class="text-ink">{segment.label}</span>
						<span class="ml-auto text-muted">{segment.value}</span>
					</li>
				{/if}
			{/each}
		</ul>
	{/if}
</div>
