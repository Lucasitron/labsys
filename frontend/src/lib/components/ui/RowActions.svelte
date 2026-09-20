<script lang="ts">
	import { onMount } from 'svelte';
	import Icon, { type IconName } from './Icon.svelte';
	import type { Tone } from '$lib/types/stock';

	export interface RowAction {
		id: string;
		label: string;
		icon?: IconName | null;
		tone?: Tone;
		disabled?: boolean;
		hidden?: boolean;
	}

	interface Props {
		actions: RowAction[];
		onSelect: (id: string) => void;
		label?: string;
	}

	let { actions, onSelect, label = 'Ações' }: Props = $props();

	let open = $state(false);
	let root: HTMLDivElement | undefined = $state();

	const visible = $derived(actions.filter((a) => !a.hidden));

	onMount(() => {
		function onDocClick(event: MouseEvent): void {
			if (root && !root.contains(event.target as Node)) open = false;
		}
		function onKey(event: KeyboardEvent): void {
			if (event.key === 'Escape') open = false;
		}
		document.addEventListener('mousedown', onDocClick);
		document.addEventListener('keydown', onKey);
		return () => {
			document.removeEventListener('mousedown', onDocClick);
			document.removeEventListener('keydown', onKey);
		};
	});

	function run(id: string): void {
		open = false;
		onSelect(id);
	}

	const tones: Partial<Record<Tone, string>> = {
		danger: 'text-danger',
		brand: 'text-brandhi',
		warn: 'text-warn',
		success: 'text-success'
	};
</script>

{#if visible.length > 0}
	<div bind:this={root} class="relative">
		<button
			type="button"
			onclick={() => (open = !open)}
			aria-expanded={open}
			aria-label={label}
			class="flex h-7 w-7 items-center justify-center rounded-md text-muted transition-colors hover:bg-border/40 hover:text-ink"
		>
			<Icon name="ellipsis-vertical" class="h-4 w-4" />
		</button>

		{#if open}
			<div
				class="absolute right-0 z-40 mt-0.5 min-w-44 overflow-hidden rounded-xl border border-border bg-elevated py-1 shadow-xl shadow-black/40"
			>
				{#each visible as action (action.id)}
					<button
						type="button"
						onclick={() => run(action.id)}
						disabled={action.disabled}
						class="flex w-full items-center gap-2 px-3 py-2 text-left text-sm transition-colors hover:bg-brand/10 disabled:cursor-not-allowed disabled:opacity-40 {tones[action.tone ?? 'ink'] ?? 'text-ink'}"
					>
						{#if action.icon}
							<Icon name={action.icon} class="h-4 w-4 shrink-0" />
						{/if}
						{action.label}
					</button>
				{/each}
			</div>
		{/if}
	</div>
{/if}