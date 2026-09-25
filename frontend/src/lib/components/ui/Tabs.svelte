<script lang="ts">
	interface Tab {
		id: string;
		label: string;
		count?: number;
	}

	interface Props {
		tabs: Tab[];
		active: string;
		onChange: (id: string) => void;
	}

	let { tabs, active, onChange }: Props = $props();
</script>

<div
	role="tablist"
	aria-label="Seções"
	class="inline-flex items-center gap-0.5 rounded-lg border border-border bg-surface p-0.5"
>
	{#each tabs as tab (tab.id)}
		<button
			role="tab"
			aria-selected={active === tab.id}
			onclick={() => onChange(tab.id)}
			class="inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors {active === tab.id
				? 'bg-brand/15 text-brandhi'
				: 'text-muted hover:text-ink'}"
		>
			{tab.label}
			{#if tab.count !== undefined}
				<span
					class="inline-flex h-4 min-w-4 items-center justify-center rounded-full px-1 text-[10px] font-bold {active === tab.id
						? 'bg-brand text-white'
						: 'bg-muted/15 text-muted'}"
				>{tab.count}</span
				>
			{/if}
		</button>
	{/each}
</div>