<script lang="ts">
	export interface BulkAction {
		label: string;
		onClick: () => void;
		danger?: boolean;
	}

	interface Props {
		count: number;
		onCancel: () => void;
		actions: BulkAction[];
	}

	let { count, onCancel, actions }: Props = $props();
</script>

<div
	class="fixed bottom-5 left-1/2 z-40 flex -translate-x-1/2 items-center gap-3 rounded-2xl border border-brand/40 bg-elevated px-4 py-2.5 shadow-xl shadow-black/50"
	role="status"
>
	<span class="text-sm font-medium text-ink">
		{count} {count === 1 ? 'item selecionado' : 'itens selecionados'}
	</span>
	<div class="flex items-center gap-1.5">
		{#each actions as action, i (i)}
			<button
				onclick={action.onClick}
				class="rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors {action.danger
					? 'bg-danger/15 text-danger hover:bg-danger/25'
					: 'bg-brand/15 text-brandhi hover:bg-brand/25'}"
			>
				{action.label}
			</button>
		{/each}
		<button
			onclick={onCancel}
			class="rounded-lg px-3 py-1.5 text-xs font-medium text-muted transition-colors hover:text-ink"
		>
			Cancelar
		</button>
	</div>
</div>