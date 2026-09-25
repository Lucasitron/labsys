<script lang="ts">
	import Icon from './Icon.svelte';

	interface Props {
		value?: string;
		placeholder?: string;
		onSearch?: (value: string) => void;
		delay?: number;
		class?: string;
		label?: string;
	}

	let {
		value = '',
		placeholder = 'Buscar…',
		onSearch,
		delay = 350,
		class: className = '',
		label = 'Buscar'
	}: Props = $props();

	let timer: ReturnType<typeof setTimeout> | undefined;

	function handleInput(event: Event): void {
		value = (event.currentTarget as HTMLInputElement).value;
		if (delay <= 0) {
			onSearch?.(value);
			return;
		}
		if (timer) clearTimeout(timer);
		timer = setTimeout(() => onSearch?.(value), delay);
	}

	function clear(): void {
		value = '';
		if (timer) clearTimeout(timer);
		onSearch?.('');
	}
</script>

<div class="relative {className}">
	<span
		class="pointer-events-none absolute inset-y-0 left-3 flex items-center text-muted"
		aria-hidden="true"
	>
		<Icon name="search" class="h-4 w-4" />
	</span>
	<label for="search-input" class="sr-only">{label}</label>
	<input
		id="search-input"
		type="search"
		{value}
		{placeholder}
		oninput={handleInput}
		class="w-full rounded-lg border border-border bg-surface py-2 pl-9 pr-8 text-sm text-ink placeholder:text-muted/70 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40"
	/>
	{#if value}
		<button
			onclick={clear}
			class="absolute inset-y-0 right-2 flex items-center text-muted transition-colors hover:text-ink"
			aria-label="Limpar busca"
		>
			<Icon name="x-mark" class="h-4 w-4" />
		</button>
	{/if}
</div>