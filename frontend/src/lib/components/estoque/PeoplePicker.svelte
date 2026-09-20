<script lang="ts">
	import { onMount } from 'svelte';
	import { searchPeople, type PersonOption } from '$lib/api/rh';
	import Avatar from './Avatar.svelte';
	import Icon from './Icon.svelte';

	interface Props {
		onSelect: (person: PersonOption | null) => void;
		selected?: PersonOption | null;
		placeholder?: string;
		disabled?: boolean;
		hint?: string;
		defaultOption?: PersonOption | null;
	}

	let {
		onSelect,
		selected = null,
		placeholder = 'Digite ao menos 2 caracteres para buscar…',
		disabled = false,
		hint = '',
		defaultOption = null
	}: Props = $props();

	let query = $state(selected?.name ?? '');
	let results = $state<PersonOption[]>([]);
	let searching = $state(false);
	let open = $state(false);
	let debounce: ReturnType<typeof setTimeout> | undefined;
	let reqId = 0;
	let root: HTMLDivElement | undefined = $state();

	onMount(() => {
		if (defaultOption && !selected) {
			selected = defaultOption;
			onSelect(defaultOption);
			query = defaultOption.name;
		}
		const onDoc = (event: MouseEvent): void => {
			if (root && !root.contains(event.target as Node)) open = false;
		};
		const onKey = (event: KeyboardEvent): void => {
			if (event.key === 'Escape') open = false;
		};
		document.addEventListener('mousedown', onDoc);
		document.addEventListener('keydown', onKey);
		return () => {
			document.removeEventListener('mousedown', onDoc);
			document.removeEventListener('keydown', onKey);
		};
	});

	async function search(q: string): Promise<void> {
		const myId = ++reqId;
		searching = true;
		try {
			const res = await searchPeople(q, fetch);
			if (myId === reqId) {
				results = res;
				open = true;
			}
		} catch {
			if (myId === reqId) results = [];
			open = true;
		} finally {
			if (myId === reqId) searching = false;
		}
	}

	function handleInput(event: Event): void {
		query = (event.currentTarget as HTMLInputElement).value;
		if (selected) {
			onSelect(null);
			selected = null;
		}
		if (debounce) clearTimeout(debounce);
		if (query.trim().length < 2) {
			results = [];
			return;
		}
		debounce = setTimeout(() => void search(query.trim()), 300);
	}

	function pick(person: PersonOption): void {
		selected = person;
		onSelect(person);
		query = person.name;
		open = false;
	}

	function clear(): void {
		selected = null;
		onSelect(null);
		query = '';
		results = [];
		open = false;
	}
</script>

<div bind:this={root} class="relative">
	{#if selected}
		<div
			class="flex items-center justify-between gap-2 rounded-lg border border-border bg-elevated px-3 py-2.5"
		>
			<span class="flex items-center gap-2">
				<Avatar name={selected.name} size="sm" />
				<span class="text-sm font-medium text-ink">{selected.name}</span>
			</span>
			{#if !disabled}
				<button
					onclick={clear}
					class="rounded-md p-1 text-muted transition-colors hover:bg-border/50 hover:text-ink"
					aria-label="Remover seleção"
				>
					<Icon name="x-mark" class="h-4 w-4" />
				</button>
			{/if}
		</div>
	{:else}
		<div class="relative">
			<span
				class="pointer-events-none absolute inset-y-0 left-3 flex items-center text-muted"
				aria-hidden="true"
			>
				<Icon name="search" class="h-4 w-4" />
			</span>
			<input
				type="search"
				{placeholder}
				bind:value={query}
				oninput={handleInput}
				{disabled}
				class="w-full rounded-lg border border-border bg-elevated py-2.5 pl-9 pr-3 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 disabled:cursor-not-allowed disabled:opacity-50"
			/>
			{#if searching}
				<span
					class="absolute inset-y-0 right-3 flex items-center text-muted"
					aria-hidden="true"
				>
					<span
						class="h-4 w-4 animate-spin rounded-full border-2 border-border border-t-brand"
					></span>
				</span>
			{/if}
			{#if hint}
				<p class="mt-1 text-xs text-muted">{hint}</p>
			{/if}
		</div>
	{/if}

	{#if open && results.length > 0}
		<div
			class="absolute left-0 right-0 z-40 mt-1 max-h-60 overflow-y-auto rounded-xl border border-border bg-elevated shadow-xl shadow-black/40"
			role="listbox"
		>
			{#each results as person (person.id)}
				<button
					type="button"
					role="option"
					onclick={() => pick(person)}
					class="flex w-full items-center gap-2.5 px-3 py-2 text-left transition-colors hover:bg-brand/10"
				>
					<Avatar name={person.name} size="sm" />
					<span class="text-sm text-ink">{person.name}</span>
				</button>
			{/each}
		</div>
	{:else if open && !searching && query.trim().length >= 2}
		<div
			class="absolute left-0 right-0 z-40 mt-1 rounded-xl border border-border bg-elevated p-4 text-sm text-muted shadow-xl shadow-black/40"
		>
			Nenhuma pessoa encontrada.
		</div>
	{/if}
</div>