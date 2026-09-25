<script lang="ts">
	import { formatNumber } from '$lib/utils/format';
	import type { Kpi } from '$lib/types/dashboard';

	interface Props {
		label: string;
		value: number;
		total?: number;
		delta?: string;
		deltaTone?: NonNullable<Kpi['deltaTone']>;
		restricted?: boolean;
		empty?: boolean;
		icon?: 'orders' | 'loans' | 'machines' | 'bell';
		error?: boolean;
	}

	let { label, value, total, delta, deltaTone = 'muted', restricted = false, empty = false, icon, error = false }: Props =
		$props();

	const TONES: Record<NonNullable<Kpi['deltaTone']>, string> = {
		success: 'text-success',
		danger: 'text-danger',
		warn: 'text-warn',
		muted: 'text-muted'
	};

	const ICON_PATHS: Record<NonNullable<Props['icon']>, string> = {
		orders:
			'M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75',
		loans:
			'M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25',
		machines:
			'M11.42 15.17L17.25 21A2.652 2.652 0 0021 17.25l-5.877-5.877M11.42 15.17l2.496-3.03c.317-.384.74-.626 1.208-.766',
		bell: 'M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0'
	};
</script>

<div data-testid="kpi-card" class="relative rounded-xl border border-border bg-surface p-4">
	{#if restricted}
		<span
			class="absolute right-2 top-2 rounded border border-brand/30 bg-brand/10 px-1 py-0.5 text-[9px] font-medium text-brand"
		>
			RESP.
		</span>
	{/if}

	<div class="mb-1 flex items-center justify-between">
		<span class="text-xs text-muted">{label}</span>
		{#if icon}
			<svg
				class="h-3.5 w-3.5 text-muted"
				fill="none"
				viewBox="0 0 24 24"
				stroke="currentColor"
				stroke-width="2"
				aria-hidden="true"
			>
				<path stroke-linecap="round" stroke-linejoin="round" d={ICON_PATHS[icon]} />
			</svg>
		{/if}
	</div>

	{#if error}
		<div class="mt-2 flex items-center gap-2 text-xs text-muted">
			<svg
				class="h-3.5 w-3.5"
				fill="none"
				viewBox="0 0 24 24"
				stroke="currentColor"
				stroke-width="2"
				aria-hidden="true"
			>
				<path
					stroke-linecap="round"
					stroke-linejoin="round"
					d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"
				/>
			</svg>
			indisponível
		</div>
	{:else}
		<div class="flex items-baseline gap-2">
			<span class="text-2xl font-semibold tracking-tight {empty ? 'text-muted' : ''}">
				{formatNumber(value)}{#if total !== undefined}<span class="text-lg text-muted"
					>/{formatNumber(total)}</span
				>{/if}
			</span>
			{#if delta}
				<span class="text-xs font-medium {TONES[deltaTone]}">{delta}</span>
			{:else if empty}
				<span class="text-xs font-medium text-muted">sem registros</span>
			{/if}
		</div>
	{/if}
</div>